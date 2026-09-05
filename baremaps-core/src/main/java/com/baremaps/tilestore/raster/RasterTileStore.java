/*
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baremaps.tilestore.raster;

import com.baremaps.dem.HillshadeCalculator;
import com.baremaps.maplibre.vectortile.Feature;
import com.baremaps.maplibre.vectortile.Layer;
import com.baremaps.maplibre.vectortile.Tile;
import com.baremaps.maplibre.vectortile.VectorTileEncoder;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.TileStoreException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.AffineTransformation;

/**
 * A read-only {@code TileStore} derived from an {@link ElevationReader}.
 *
 * <p>
 * The elevation is a source, not a store: tiles are computed on demand and there is nothing to
 * write to or delete from.
 *
 * @param <T> the type of the tiles
 */
abstract class RasterTileStore<T> implements TileStore<T> {

  /** The side of a tile in pixels. */
  protected static final int TILE_SIZE = 256;

  /** The extent a vector tile's coordinates are expressed in. */
  protected static final int TILE_EXTENT = 4096;

  /**
   * The border, in pixels, that a traced contour needs on every side.
   *
   * <p>
   * A contour that leaves a tile has to arrive at the same place in the adjacent tile, so the grid
   * it is traced from is computed wider than the tile and translated back afterwards. Sixteen
   * pixels is what the widest shading step needs; the elevation contours need less and share it, so
   * that both can be traced from a single grid.
   */
  protected static final int TRACE_BUFFER = 16;

  /**
   * How many samples a tile is traced with per pixel of elevation the source actually holds.
   *
   * <p>
   * Past the depth of the elevation, a finer grid is interpolation rather than detail, and tracing
   * it shows. A shade follows the gradient, and the gradient of an interpolated surface breaks at
   * the edge of every real pixel, so a grid asked for eight times finer than the data draws the
   * shape of those pixels onto the hill. Tracing near the resolution the data has instead gives a
   * deep tile the relief a shallow one is traced with, which is what a client stretching a
   * shallower tile used to show, and it costs less work rather than more.
   *
   * <p>
   * Two rather than one, because that is the ratio the shallower tiles being reproduced were traced
   * at, and a grid on the data's own pixels resolves nothing that falls between them.
   */
  private static final int OVERSAMPLING = 2;

  /** The coarsest grid a tile is traced from, below which a tile is a handful of samples. */
  private static final int MIN_TRACE_SIZE = 16;

  protected final ElevationReader elevation;

  protected RasterTileStore(ElevationReader elevation) {
    this.elevation = elevation;
  }

  /**
   * The elevation a tile is traced from: the values, the side of the tile in samples, and the
   * border read around it.
   *
   * @param values the elevation in meters, row-major, {@link #side()} on a side
   * @param size the side of the tile itself, in samples
   * @param buffer the border read on every side, in samples
   */
  protected record Grid(double[] values, int size, int buffer) {

    /** The side of the grid, which is the tile and its border on both sides. */
    int side() {
      return size + 2 * buffer;
    }

    /**
     * The ground distance one sample covers, in meters, halved.
     *
     * <p>
     * Halved because relief drawn at the gradient the ground actually has is too faint to read at
     * these scales: the shading is exaggerated, and it is exaggerated by the same amount at every
     * zoom and every grid size, so that a hill looks like itself wherever it is met.
     */
    double cellSize(int zoom) {
      return HillshadeCalculator.getResolution(zoom) * TILE_SIZE / size / 2;
    }

    /** Moves the border out of the way and scales the grid to the extent of a vector tile. */
    AffineTransformation toTileExtent() {
      return AffineTransformation
          .translationInstance(-buffer, -buffer)
          .scale((double) TILE_EXTENT / size, (double) TILE_EXTENT / size);
    }
  }

  /**
   * Reads the elevation a tile is traced from, at the resolution the source has for it.
   *
   * @param tileCoord the tile coordinate
   * @return the grid
   * @throws TileStoreException if the elevation cannot be read
   */
  protected Grid grid(TileCoord tileCoord) throws TileStoreException {
    var available = (long) elevation.resolution(tileCoord.z()) * OVERSAMPLING;
    var size = (int) Math.clamp(
        Long.highestOneBit(Math.min(available, TILE_SIZE)), MIN_TRACE_SIZE, TILE_SIZE);
    // The border covers the same ground whatever the grid is, so it is scaled with it.
    var buffer = Math.max(1, TRACE_BUFFER * size / TILE_SIZE);
    return new Grid(elevation.read(tileCoord, size, buffer), size, buffer);
  }

  /**
   * Whether a polygon still encloses anything once its coordinates are rounded to the integers a
   * vector tile is written in.
   *
   * <p>
   * Tracing real elevation produces slivers a fraction of a tile unit across. Rounding flattens
   * those onto a single row or column, leaving a ring of zero area. Such a ring draws nothing, and
   * it is worse than merely wasteful: a decoder tells a polygon's outline from its holes by the
   * sign of that area, so a feature whose outline has collapsed is read as a feature with no
   * outline at all. Dropping them here costs nothing that could have been seen.
   */
  protected static boolean drawable(Polygon polygon) {
    return roundedArea(polygon.getExteriorRing()) > 0;
  }

  /** Twice the area of a ring, with every coordinate rounded the way the encoder rounds it. */
  private static double roundedArea(LinearRing ring) {
    var coordinates = ring.getCoordinates();
    double area = 0;
    for (int i = 0; i < coordinates.length - 1; i++) {
      area += (double) Math.round(coordinates[i].x) * Math.round(coordinates[i + 1].y)
          - (double) Math.round(coordinates[i + 1].x) * Math.round(coordinates[i].y);
    }
    return Math.abs(area);
  }

  /** Encodes features as a single-layer vector tile, gzipped as the tile protocol expects. */
  protected static ByteBuffer encodeLayer(String name, List<Feature> features) throws IOException {
    return encodeLayers(List.of(new Layer(name, TILE_EXTENT, features)));
  }

  /**
   * Encodes several layers as one vector tile, gzipped as the tile protocol expects.
   *
   * <p>
   * Subjects computed from the same elevation grid travel together rather than as one source each,
   * so that a client fetches them once and reads the grid once.
   */
  protected static ByteBuffer encodeLayers(List<Layer> layers) throws IOException {
    var vectorTile = new VectorTileEncoder().encodeTile(new Tile(layers));
    try (var bytes = new ByteArrayOutputStream()) {
      try (var gzip = new GZIPOutputStream(bytes)) {
        vectorTile.writeTo(gzip);
      }
      return ByteBuffer.wrap(bytes.toByteArray());
    }
  }

  /** This store is read-only: the tiles are derived from the elevation data. */
  @Override
  public void write(TileCoord tileCoord, T blob) throws TileStoreException {
    throw new UnsupportedOperationException("The raster tile store is read only");
  }

  /** This store is read-only: the tiles are derived from the elevation data. */
  @Override
  public void delete(TileCoord tileCoord) throws TileStoreException {
    throw new UnsupportedOperationException("The raster tile store is read only");
  }

  /**
   * Does nothing: the reader is owned by the caller that opened it, which commonly shares one
   * reader between several of these stores.
   */
  @Override
  public void close() {
    // Do nothing
  }
}
