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
import com.baremaps.maplibre.map.MapSpec;
import com.baremaps.maplibre.vectortile.Feature;
import com.baremaps.maplibre.vectortile.Layer;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStoreException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A {@code TileStore} that traces vector contour tiles from elevation.
 *
 * <p>
 * A contour is a line and is traced as one, not as the boundary of the ground above it. Both would
 * draw the same stroke, but only a line can be labelled with the height it stands for: a renderer
 * asked to write along a shape needs to know which way along it to write, and a closed ring around
 * a summit does not say. Tracing lines also leaves out what a ring needs and a contour does not,
 * the segments that close it against the edge of the grid and the nesting of one inside another.
 */
public class VectorContourTileStore extends RasterTileStore<ByteBuffer> {

  /** The name of the layer the contours are encoded in, which the map format states. */
  static final String LAYER = MapSpec.Terrain.CONTOUR;

  /**
   * How many intervals apart the contours a map draws more heavily are. Which contour carries a
   * label and a thicker line is a decision the tracer can make and the style cannot, because the
   * interval changes with the zoom level and a style filter would have to change with it.
   */
  private static final int INDEX_EVERY = 5;

  /**
   * Constructs a {@code VectorContourTileStore} with the specified elevation reader.
   *
   * @param elevation the elevation reader
   */
  public VectorContourTileStore(ElevationReader elevation) {
    super(elevation);
  }

  /**
   * Read the contour data for the specified tile coordinate.
   *
   * @param tileCoord the tile coordinate
   * @return the contour data
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
   * Traces the contours of an elevation grid into a vector tile layer.
   *
   * @param grid the elevation the tile is traced from, including the border
   * @param zoom the zoom level the tile is produced at, which sets the interval
   * @return the layer
   */
  static Layer layer(Grid grid, int zoom) {
    var values = grid.values();
    var tracer = new ContourTracer(values, grid.side(), grid.side());
    var toTileExtent = grid.toTileExtent();

    // Only the levels the grid actually crosses are traced. Walking the whole range of terrestrial
    // elevations instead would sweep the grid two thousand times at the zoom levels where the
    // interval is finest, to find that the tile spans a hundred meters of it.
    var interval = interval(zoom);
    var min = Double.MAX_VALUE;
    var max = -Double.MAX_VALUE;
    for (var value : values) {
      min = Math.min(min, value);
      max = Math.max(max, value);
    }
    var lowest = (int) (Math.ceil(min / interval) * interval);
    var highest = (int) (Math.floor(max / interval) * interval);

    var features = new ArrayList<Feature>();
    for (int level = lowest; level <= highest; level += interval) {
      var tags = new HashMap<String, Object>();
      tags.put("level", String.valueOf(level));
      if (Math.floorMod(level / interval, INDEX_EVERY) == 0) {
        tags.put("index", "yes");
      }
      for (var line : tracer.traceLines(level)) {
        features.add(new Feature(level, tags, toTileExtent.transform(line)));
      }
    }
    return new Layer(LAYER, TILE_EXTENT, features);
  }

  /**
   * Returns the elevation between two contours at a zoom level. A tile covers a smaller area as the
   * zoom grows, so the contours can be drawn closer together without crowding it.
   *
   * <p>
   * What sets the interval is not the zoom on its own but how far apart the contours land on the
   * screen, and that depends on the slope: the steeper the ground, the closer together the same
   * interval draws them. The values below keep a slope of forty-five degrees, which is a mountain
   * face rather than a hillside, at roughly eight pixels between contours. A gentler interval reads
   * better on gentle ground and fills a cliff with solid ink, which is what these are chosen to
   * avoid: at a hundred meters, the north faces of the Alps come out black.
   *
   * @param zoom the zoom level
   * @return the interval in meters
   */
  static int interval(int zoom) {
    return switch (zoom) {
      case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 -> 1000;
      case 10, 11 -> 500;
      case 12, 13 -> 200;
      case 14, 15 -> 100;
      default -> 50;
    };
  }
}
