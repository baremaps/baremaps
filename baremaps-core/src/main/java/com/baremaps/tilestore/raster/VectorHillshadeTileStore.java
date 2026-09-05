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
import com.baremaps.maplibre.map.MapSpec;
import com.baremaps.maplibre.vectortile.Feature;
import com.baremaps.maplibre.vectortile.Layer;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStoreException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Polygon;

/**
 * A {@code TileStore} that calculates vector hillshade tiles from elevation data.
 *
 * <p>
 * The shading is expressed as nested contours of the hillshade grid rather than as pixels, so that
 * a client can style the levels itself. A raster shade carries its own colours and its own opacity
 * and cannot be told to suit a dark map or a colour-vision theme; six nested polygons can.
 */
public class VectorHillshadeTileStore extends RasterTileStore<ByteBuffer> {

  /** The name of the layer the shading is encoded in, which the map format states. */
  static final String LAYER = MapSpec.Terrain.HILLSHADE;

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
      return encodeLayers(List.of(layer(grid(tileCoord), tileCoord.z())));
    } catch (TileStoreException e) {
      throw e;
    } catch (Exception e) {
      throw new TileStoreException(e);
    }
  }

  /**
   * Shades an elevation grid and traces the result into a vector tile layer.
   *
   * @param grid the elevation the tile is traced from, including the border
   * @param zoom the zoom level the tile is produced at, which sets the size of a sample
   * @return the layer
   */
  static Layer layer(Grid grid, int zoom) {
    var side = grid.side();
    var shaded = new HillshadeCalculator(
        ElevationUtils.clampGrid(grid.values(), 0, 10000),
        side,
        side,
        grid.cellSize(zoom))
            .calculate(ALTITUDE, AZIMUTH);

    var features = new ArrayList<Feature>();
    addContours(shaded, grid, LIT_LEVELS, features);
    addContours(HillshadeCalculator.invert(shaded), grid, SHADED_LEVELS, features);
    return new Layer(LAYER, TILE_EXTENT, features);
  }

  private static void addContours(double[] shaded, Grid grid, int[][] levels,
      List<Feature> features) {
    var tracer = new ContourTracer(shaded, grid.side(), grid.side());
    var toTileExtent = grid.toTileExtent();
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
