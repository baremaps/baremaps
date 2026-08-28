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
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.TileStoreException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.IntToDoubleFunction;

/**
 * A {@code TileStore} that calculates hillshade tiles from elevation tiles.
 */
public class RasterHillshadeTileStore implements TileStore<BufferedImage> {

  private final GeoTiffReader geoTiffReader;

  private final IntToDoubleFunction pixelToElevation;

  /**
   * Constructs a {@code RasterHillshadeTileStore} with the specified tile store and pixel to
   * elevation function.
   *
   * @param geoTiffReader the geotiff reader
   * @param pixelToElevation the pixel to elevation function
   */
  public RasterHillshadeTileStore(
      GeoTiffReader geoTiffReader,
      IntToDoubleFunction pixelToElevation) {
    this.geoTiffReader = geoTiffReader;
    this.pixelToElevation = pixelToElevation;
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
      var tileSize = 256;
      var tileBuffer = 1;
      var imageSize = tileSize + tileBuffer + tileBuffer;

      // Read the elevation data
      var grid = geoTiffReader.read(tileCoord, tileSize, tileBuffer);
      grid = ElevationUtils.clampGrid(grid, 0, 10000);
      grid = new HillshadeCalculator(
          grid,
          imageSize,
          imageSize,
          HillshadeCalculator.getResolution(tileCoord.z()) / 2)
              .calculate(45, 315);

      // Create the hillshade image
      BufferedImage hillshadeImage =
          new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_BYTE_GRAY);
      for (int y = 0; y < imageSize; y++) {
        for (int x = 0; x < imageSize; x++) {
          int value = (int) grid[y * imageSize + x];
          hillshadeImage.setRGB(x, y, new Color(value, value, value).getRGB());
        }
      }

      // Return the hillshade image without the buffer
      return hillshadeImage.getSubimage(1, 1, tileSize, tileSize);
    } catch (Exception e) {
      throw new TileStoreException(e);
    }
  }

  /**
   * Unsupported operation.
   */
  @Override
  public void write(TileCoord tileCoord, BufferedImage blob) throws TileStoreException {
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
