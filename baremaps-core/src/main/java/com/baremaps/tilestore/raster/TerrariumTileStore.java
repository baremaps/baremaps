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
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.TileStoreException;
import java.awt.image.BufferedImage;

/**
 * A {@code TileStore} that reads elevation tiles from a GeoTIFF file and converts them to terrarium
 * tiles format.
 */
public class TerrariumTileStore implements TileStore<BufferedImage> {

  private final GeoTiffReader geoTiffReader;

  /**
   * Constructs a {@code TerrariumTileStore} with the specified GeoTIFF reader.
   *
   * @param geoTiffReader
   */
  public TerrariumTileStore(GeoTiffReader geoTiffReader) {
    this.geoTiffReader = geoTiffReader;
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
      var size = 256;
      var grid = geoTiffReader.read(tileCoord, size, 0);
      var bufferedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
      for (int x = 0; x < size; x++) {
        for (int y = 0; y < size; y++) {
          double value = (int) grid[y * size + x];
          int rgb = ElevationUtils.elevationToTerrarium(value);
          bufferedImage.setRGB(x, y, rgb);
        }
      }
      return bufferedImage;
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

  /**
   * Unsupported operation.
   */
  @Override
  public void close() throws Exception {
    this.geoTiffReader.close();
  }
}
