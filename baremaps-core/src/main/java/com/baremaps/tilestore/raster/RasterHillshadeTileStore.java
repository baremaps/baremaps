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
import com.baremaps.dem.HillshadeCalculator;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStoreException;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * A {@code TileStore} that calculates hillshade tiles from elevation tiles.
 */
public class RasterHillshadeTileStore extends RasterTileStore<BufferedImage> {

  // The shade of a border pixel depends on its neighbours, so the tile is computed one pixel wider
  // on every side and the border is cropped away before the tile is returned.
  private static final int TILE_BUFFER = 1;

  /**
   * Constructs a {@code RasterHillshadeTileStore} with the specified GeoTIFF reader.
   *
   * @param geoTiffReader the geotiff reader
   */
  public RasterHillshadeTileStore(GeoTiffReader geoTiffReader) {
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
  public BufferedImage read(TileCoord tileCoord) throws TileStoreException {
    try {
      var imageSize = TILE_SIZE + 2 * TILE_BUFFER;

      var grid = geoTiffReader.read(tileCoord, TILE_SIZE, TILE_BUFFER);
      grid = ElevationUtils.clampGrid(grid, 0, 10000);
      grid = new HillshadeCalculator(
          grid,
          imageSize,
          imageSize,
          HillshadeCalculator.getResolution(tileCoord.z()) / 2)
              .calculate(45, 315);

      var image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_BYTE_GRAY);
      for (int y = 0; y < imageSize; y++) {
        for (int x = 0; x < imageSize; x++) {
          int value = (int) grid[y * imageSize + x];
          image.setRGB(x, y, new Color(value, value, value).getRGB());
        }
      }

      return image.getSubimage(TILE_BUFFER, TILE_BUFFER, TILE_SIZE, TILE_SIZE);
    } catch (Exception e) {
      throw new TileStoreException(e);
    }
  }
}
