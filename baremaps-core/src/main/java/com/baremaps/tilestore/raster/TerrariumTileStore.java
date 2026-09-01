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

import com.baremaps.dem.ElevationUtils;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStoreException;
import java.awt.image.BufferedImage;

/**
 * A {@code TileStore} that renders elevation as terrarium encoded raster tiles.
 */
public class TerrariumTileStore extends RasterTileStore<BufferedImage> {

  /**
   * Constructs a {@code TerrariumTileStore} with the specified elevation reader.
   *
   * @param elevation the elevation reader
   */
  public TerrariumTileStore(ElevationReader elevation) {
    super(elevation);
  }

  /**
   * Read the elevation data for the specified tile coordinate.
   *
   * @param tileCoord the tile coordinate
   * @return the elevation data
   * @throws TileStoreException if an error occurs
   */
  @Override
  public BufferedImage read(TileCoord tileCoord) throws TileStoreException {
    try {
      var grid = elevation.read(tileCoord, TILE_SIZE, 0);
      var image = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
      for (int x = 0; x < TILE_SIZE; x++) {
        for (int y = 0; y < TILE_SIZE; y++) {
          image.setRGB(x, y, ElevationUtils.elevationToTerrarium((int) grid[y * TILE_SIZE + x]));
        }
      }
      return image;
    } catch (Exception e) {
      throw new TileStoreException(e);
    }
  }
}
