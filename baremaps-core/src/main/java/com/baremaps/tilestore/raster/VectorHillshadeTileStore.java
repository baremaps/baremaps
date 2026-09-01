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
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStoreException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.AffineTransformation;

/**
 * A {@code TileStore} that calculates vector hillshade tiles from elevation data.
 *
 * <p>
 * The shading is expressed as nested contours of the hillshade grid rather than as pixels, so that
 * a client can style the levels itself. A raster shade carries its own colours and its own opacity
 * and cannot be told to suit a dark map or a colour-vision theme; six nested polygons can.
 */
public class VectorHillshadeTileStore extends RasterTileStore<ByteBuffer> {

  /** The name of the layer the shading is encoded in. */
  static final String LAYER = "hillshade";

  // The levels of the lit and of the shaded side, from the faintest to the strongest, paired with
  // the category a client styles them by.
  private static final int[][] LIT_LEVELS = {{255 - 16, 1}, {255 - 32, 2}};

  private static final int[][] SHADED_LEVELS =
      {{255 - 32, 6}, {255 - 64, 5}, {255 - 98, 4}, {255 - 128, 3}};

  /** The direction the light comes from, and how high it stands, in degrees. */
  private static final double ALTITUDE = 45;

  private static final double AZIMUTH = 315;

  /**
   * Constructs a {@code VectorHillshadeTileStore} with the specified elevation reader.
   *
   * @param elevation the elevation reader
   */
  public VectorHillshadeTileStore(ElevationReader elevation) {
    super(elevation);
  }

  /**
   * Read the hillshade data for the specified tile coordinate.
   *
   * @param tileCoord the tile coordinate
   * @return the hillshade data
   * @throws TileStoreException if an error occurs
   */
  @Override
  public ByteBuffer read(TileCoord tileCoord) throws TileStoreException {
    try {
      var grid = elevation.read(tileCoord, TILE_SIZE, TRACE_BUFFER);
      return encodeLayers(List.of(layer(grid, tileCoord.z())));
    } catch (TileStoreException e) {
      throw e;
    } catch (Exception e) {
      throw new TileStoreException(e);
    }
  }

  /**
   * Shades an elevation grid and traces the result into a vector tile layer.
   *
   * @param grid the elevation grid, including the border
   * @param zoom the zoom level the tile is produced at, which sets the size of a pixel
   * @return the layer
   */
  static Layer layer(double[] grid, int zoom) {
    var gridSize = TILE_SIZE + 2 * TRACE_BUFFER;
    var shaded = new HillshadeCalculator(
        ElevationUtils.clampGrid(grid, 0, 10000),
        gridSize,
        gridSize,
        HillshadeCalculator.getResolution(zoom) / 2)
            .calculate(ALTITUDE, AZIMUTH);

    var features = new ArrayList<Feature>();
    addContours(shaded, gridSize, LIT_LEVELS, features);
    addContours(HillshadeCalculator.invert(shaded), gridSize, SHADED_LEVELS, features);
    return new Layer(LAYER, TILE_EXTENT, features);
  }

  private static void addContours(double[] grid, int gridSize, int[][] levels,
      List<Feature> features) {
    var tracer = new ContourTracer(grid, gridSize, gridSize);
    // Move the border out of the way, then scale the grid to the extent of the vector tile.
    var toTileExtent = AffineTransformation
        .translationInstance(-TRACE_BUFFER, -TRACE_BUFFER)
        .scale((double) TILE_EXTENT / TILE_SIZE, (double) TILE_EXTENT / TILE_SIZE);
    for (int[] level : levels) {
      var category = level[1];
      for (var polygon : tracer.tracePolygons(level[0])) {
        var scaled = (Polygon) toTileExtent.transform(polygon);
        if (drawable(scaled)) {
          features.add(new Feature(category, Map.of("level", String.valueOf(category)), scaled));
        }
      }
    }
  }
}
