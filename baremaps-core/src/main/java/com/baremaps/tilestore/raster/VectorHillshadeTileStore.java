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
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStoreException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.util.AffineTransformation;

/**
 * A {@code TileStore} that calculates vector hillshade tiles from elevation data.
 *
 * <p>
 * The shading is expressed as nested contours of the hillshade grid rather than as pixels, so that
 * a client can style the levels itself.
 */
public class VectorHillshadeTileStore extends RasterTileStore<ByteBuffer> {

  // Contours near the edge of a tile need the neighbouring elevations to line up with the contours
  // of the adjacent tile, so the grid is computed with a wide border that is then translated away.
  private static final int TILE_BUFFER = 16;

  // The levels of the lit and of the shaded side, from the faintest to the strongest, paired with
  // the category a client styles them by.
  private static final int[][] LIT_LEVELS = {{255 - 16, 1}, {255 - 32, 2}};

  private static final int[][] SHADED_LEVELS =
      {{255 - 32, 6}, {255 - 64, 5}, {255 - 98, 4}, {255 - 128, 3}};

  /**
   * Constructs a {@code VectorHillshadeTileStore} with the specified geotiff reader.
   *
   * @param geoTiffReader the geotiff reader
   */
  public VectorHillshadeTileStore(GeoTiffReader geoTiffReader) {
    super(geoTiffReader);
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
      var gridSize = TILE_SIZE + 2 * TILE_BUFFER;

      var grid = geoTiffReader.read(tileCoord, TILE_SIZE, TILE_BUFFER);
      grid = ElevationUtils.clampGrid(grid, 0, 10000);
      grid = new HillshadeCalculator(
          grid,
          gridSize,
          gridSize,
          HillshadeCalculator.getResolution(tileCoord.z()) / 2)
              .calculate(45, 315);

      var features = new ArrayList<Feature>();
      addContours(grid, gridSize, LIT_LEVELS, features);
      addContours(HillshadeCalculator.invert(grid), gridSize, SHADED_LEVELS, features);

      return encodeLayer("elevation", features);
    } catch (Exception e) {
      throw new TileStoreException(e);
    }
  }

  private static void addContours(double[] grid, int gridSize, int[][] levels,
      List<Feature> features) {
    var tracer = new ContourTracer(grid, gridSize, gridSize);
    // Move the border out of the way, then scale the grid to the extent of the vector tile.
    var toTileExtent = AffineTransformation
        .translationInstance(-TILE_BUFFER, -TILE_BUFFER)
        .scale(4096 / TILE_SIZE, 4096 / TILE_SIZE);
    for (int[] level : levels) {
      var category = level[1];
      for (var polygon : tracer.tracePolygons(level[0])) {
        features.add(new Feature(category, Map.of("level", String.valueOf(category)),
            toTileExtent.transform(polygon)));
      }
    }
  }
}
