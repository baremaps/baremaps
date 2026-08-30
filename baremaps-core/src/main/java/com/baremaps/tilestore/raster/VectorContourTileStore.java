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
import com.baremaps.maplibre.vectortile.Feature;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStoreException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import org.locationtech.jts.geom.util.AffineTransformation;

/**
 * A {@code TileStore} that calculates vector contour tiles from elevation tiles.
 */
public class VectorContourTileStore extends RasterTileStore<ByteBuffer> {

  // A contour that runs off the edge of a tile has to meet the one of the adjacent tile, so the
  // grid is computed with a border that is then translated away.
  private static final int TILE_BUFFER = 4;

  // The lowest and highest elevations on earth, rounded outwards.
  private static final int MIN_ELEVATION = -10000;

  private static final int MAX_ELEVATION = 10000;

  /**
   * Constructs a {@code VectorContourTileStore} with the specified GeoTIFF reader.
   *
   * @param geoTiffReader the geotiff reader
   */
  public VectorContourTileStore(GeoTiffReader geoTiffReader) {
    super(geoTiffReader);
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
      var gridSize = TILE_SIZE + 2 * TILE_BUFFER;
      var grid = geoTiffReader.read(tileCoord, TILE_SIZE, TILE_BUFFER);

      var tracer = new ContourTracer(grid, gridSize, gridSize);
      // Move the border out of the way, then scale the grid to the extent of the vector tile.
      var toTileExtent = AffineTransformation
          .translationInstance(-TILE_BUFFER, -TILE_BUFFER)
          .scale(4096 / TILE_SIZE, 4096 / TILE_SIZE);

      var features = new ArrayList<Feature>();
      for (int level = MIN_ELEVATION; level < MAX_ELEVATION; level += interval(tileCoord.z())) {
        for (var polygon : tracer.tracePolygons(level)) {
          features.add(new Feature(level, Map.of("level", String.valueOf(level)),
              toTileExtent.transform(polygon)));
        }
      }

      return encodeLayer("contour", features);
    } catch (Exception e) {
      throw new TileStoreException(e);
    }
  }

  /**
   * Returns the elevation between two contours at a zoom level. A tile covers a smaller area as the
   * zoom grows, so the contours can be drawn closer together without crowding it.
   *
   * @param zoom the zoom level
   * @return the interval in meters
   */
  private static int interval(int zoom) {
    return switch (zoom) {
      case 1, 2 -> 2000;
      case 3, 4, 5, 6, 7 -> 1000;
      case 8, 9 -> 500;
      case 10, 11 -> 250;
      case 12, 13 -> 100;
      case 14 -> 50;
      default -> 10;
    };
  }
}
