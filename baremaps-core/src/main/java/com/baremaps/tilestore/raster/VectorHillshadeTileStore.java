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

import com.baremaps.dem.ContourTracer;
import com.baremaps.dem.ElevationUtils;
import com.baremaps.dem.HillshadeCalculator;
import com.baremaps.maplibre.vectortile.Feature;
import com.baremaps.maplibre.vectortile.Layer;
import com.baremaps.maplibre.vectortile.Tile;
import com.baremaps.maplibre.vectortile.VectorTileEncoder;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.TileStoreException;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.locationtech.jts.geom.util.AffineTransformation;

/**
 * A {@code TileStore} that calculates vector hillshade tiles from elevation data.
 */
public class VectorHillshadeTileStore implements TileStore<ByteBuffer> {

  private final GeoTiffReader geoTiffReader;

  /**
   * Constructs a {@code VectorHillshadeTileStore} with the specified geotiff reader.
   *
   * @param geoTiffReader the geotiff reader
   */
  public VectorHillshadeTileStore(GeoTiffReader geoTiffReader) {
    this.geoTiffReader = geoTiffReader;
  }

  /**
   * Read the hillshade data for the specified tile coordinate.
   *
   * @param tileCoord the tile coordinate
   * @return the hillshade data
   * @throws TileStoreException
   */
  @Override
  public ByteBuffer read(TileCoord tileCoord) throws TileStoreException {
    try {
      var size = 256;

      // Read the elevation data
      var features = new ArrayList<Feature>();

      // Calculate the hillshade
      var grid = geoTiffReader.read(tileCoord, size, 16);
      grid = ElevationUtils.clampGrid(grid, 0, 10000);
      grid = new HillshadeCalculator(
          grid,
          size + 32, size + 32, HillshadeCalculator.getResolution(tileCoord.z()) / 2)
              .calculate(45, 315);

      contours(grid, 255 - 16, features, 1);
      contours(grid, 255 - 32, features, 2);

      grid = ElevationUtils.invertGrid(grid);
      contours(grid, 255 - 32, features, 6);
      contours(grid, 255 - 64, features, 5);
      contours(grid, 255 - 98, features, 4);
      contours(grid, 255 - 128, features, 3);

      var layer = new Layer("elevation", 4096, features);
      var tile = new Tile(List.of(layer));
      var vectorTile = new VectorTileEncoder().encodeTile(tile);
      try (var baos = new ByteArrayOutputStream()) {
        var gzip = new GZIPOutputStream(baos);
        vectorTile.writeTo(gzip);
        gzip.close();
        return ByteBuffer.wrap(baos.toByteArray());
      }
    } catch (Exception e) {
      throw new TileStoreException(e);
    }
  }

  private static void contours(double[] grid, int level, ArrayList<Feature> features,
      int category) {
    var contours =
        new ContourTracer(grid, (int) Math.sqrt(grid.length), (int) Math.sqrt(grid.length), false,
            true)
                .traceContours(level);
    for (var contour : contours) {
      contour = AffineTransformation
          .translationInstance(-16, -16)
          .scale(16, 16)
          .transform(contour);
      features.add(new Feature(category, Map.of("level", String.valueOf(category)), contour));
    }
  }

  /**
   * Unsupported operation.
   */
  @Override
  public void write(TileCoord tileCoord, ByteBuffer blob) throws TileStoreException {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported operation.
   */
  @Override
  public void delete(TileCoord tileCoord) throws TileStoreException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws Exception {
    // Do nothing
  }
}
