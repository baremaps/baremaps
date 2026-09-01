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

import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStoreException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * A {@code TileStore} that produces the shading and the elevation contours of a tile together, as
 * the {@code hillshade} and {@code contour} layers of one vector tile.
 *
 * <p>
 * They are one tile rather than two sources because they are one computation. Both are traced from
 * the same elevation grid, which is the expensive part of producing either: reading it once halves
 * the work on the server and the requests in the browser, and guarantees that the shading and the
 * contours of a tile were derived from the same elevation rather than from two reads that a
 * changing archive could separate.
 */
public class VectorTerrainTileStore extends RasterTileStore<ByteBuffer> {

  /**
   * Constructs a {@code VectorTerrainTileStore} with the specified elevation reader.
   *
   * @param elevation the elevation reader
   */
  public VectorTerrainTileStore(ElevationReader elevation) {
    super(elevation);
  }

  /**
   * Reads the shading and the contours of the specified tile coordinate.
   *
   * @param tileCoord the tile coordinate
   * @return the vector tile
   * @throws TileStoreException if an error occurs
   */
  @Override
  public ByteBuffer read(TileCoord tileCoord) throws TileStoreException {
    try {
      var grid = elevation.read(tileCoord, TILE_SIZE, TRACE_BUFFER);
      return encodeLayers(List.of(
          VectorHillshadeTileStore.layer(grid, tileCoord.z()),
          VectorContourTileStore.layer(grid, tileCoord.z())));
    } catch (TileStoreException e) {
      throw e;
    } catch (Exception e) {
      throw new TileStoreException(e);
    }
  }
}
