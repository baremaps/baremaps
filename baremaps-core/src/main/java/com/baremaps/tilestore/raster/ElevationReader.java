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

/**
 * A source of elevation, sampled over the extent of a tile.
 *
 * <p>
 * The grid comes back square and in row-major order, {@code tileSizePx + 2 * tileBufferPx} on a
 * side, in the web mercator projection and in meters.
 *
 * <p>
 * The border is the reason this is one call rather than a read per pixel. Everything derived from
 * elevation — a contour, a slope, a shade — depends on the values around a pixel as much as on the
 * pixel itself, so a tile computed from its own extent alone disagrees with its neighbours along
 * the edge they share. Only the source knows what lies beyond the tile, so the caller asks for the
 * border instead of inventing one.
 *
 * <p>
 * What the elevation is stored as is the implementation's business: a single GeoTIFF covering a
 * region, or a pyramid of encoded raster tiles covering the planet, resolve to the same grid.
 */
public interface ElevationReader extends AutoCloseable {

  /**
   * Reads the elevation over the extent of a tile, widened by a border.
   *
   * @param tileCoord the tile coordinate
   * @param tileSizePx the side of the tile, in pixels
   * @param tileBufferPx the border added on every side, in pixels
   * @return the elevation in meters, row-major, {@code tileSizePx + 2 * tileBufferPx} on a side
   * @throws TileStoreException if the elevation cannot be read
   */
  double[] read(TileCoord tileCoord, int tileSizePx, int tileBufferPx) throws TileStoreException;

  /**
   * How many pixels across a tile at this zoom the elevation has values of its own for.
   *
   * <p>
   * A source runs out of detail at some depth, and past it every extra sample asked for is
   * interpolated from the same few values. What is read back is smooth but it is not finer, and a
   * caller that traces the grid rather than displaying it can tell: a shade follows the gradient,
   * and interpolation breaks the gradient at the edge of every real pixel. So a caller asks how
   * much there is to have before asking for it.
   *
   * <p>
   * A source that cannot say offers as much as it is asked for, which is what a caller reading a
   * grid to display it wants anyway.
   *
   * @param zoom the zoom level
   * @return the side, in pixels, of the finest grid worth asking for over one tile
   */
  default int resolution(int zoom) {
    return Integer.MAX_VALUE;
  }
}
