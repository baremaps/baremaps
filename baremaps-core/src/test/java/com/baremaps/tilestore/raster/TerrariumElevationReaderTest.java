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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.dem.ElevationUtils;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.TileStoreException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleBiFunction;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * The reader is exercised with PNG tiles rather than the WebP ones the terrain archives ship,
 * because the image format is {@link ImageIO}'s business and the sampling is this class's. What is
 * checked here is that a value written into a source tile comes back at the place the tiling scheme
 * puts it.
 */
class TerrariumElevationReaderTest {

  private static final int TILE_SIZE = 8;

  /** An archive whose tiles are drawn by a function of their position in the world. */
  private static class Archive implements TileStore<ByteBuffer> {

    private final Map<TileCoord, ByteBuffer> tiles = new HashMap<>();

    /** Draws every tile of a zoom level, colouring a pixel by its world coordinates. */
    Archive(int zoom, ToDoubleBiFunction<Long, Long> elevation) {
      for (int x = 0; x < 1 << zoom; x++) {
        for (int y = 0; y < 1 << zoom; y++) {
          var image = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
          for (int i = 0; i < TILE_SIZE; i++) {
            for (int j = 0; j < TILE_SIZE; j++) {
              var value = elevation.applyAsDouble(
                  (long) x * TILE_SIZE + i, (long) y * TILE_SIZE + j);
              image.setRGB(i, j, ElevationUtils.elevationToTerrarium(value));
            }
          }
          tiles.put(new TileCoord(x, y, zoom), encode(image));
        }
      }
    }

    private static ByteBuffer encode(BufferedImage image) {
      try (var bytes = new ByteArrayOutputStream()) {
        ImageIO.write(image, "png", bytes);
        return ByteBuffer.wrap(bytes.toByteArray());
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public ByteBuffer read(TileCoord tileCoord) {
      return tiles.get(tileCoord);
    }

    @Override
    public void write(TileCoord tileCoord, ByteBuffer blob) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void delete(TileCoord tileCoord) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void close() {}
  }

  /**
   * A tile read at the resolution its own level holds is the source pixels themselves. A target
   * tile of four pixels at zoom two sits inside a source tile of eight at zoom one, so the values
   * must be those of its quadrant, exactly.
   */
  @Test
  void readsTheSourcePixelsWhenTheResolutionsMatch() throws Exception {
    // The elevation is the source pixel's own column, so a returned value names where it came from.
    var archive = new Archive(1, (x, y) -> x);
    try (var reader = new TerrariumElevationReader(archive, TILE_SIZE, 1)) {
      var grid = reader.read(new TileCoord(3, 2, 2), 4, 0);
      assertEquals(16, grid.length);
      // Tile 3 of zoom 2 is the second half of tile 1 of zoom 1: source columns 12 to 15.
      for (int j = 0; j < 4; j++) {
        for (int i = 0; i < 4; i++) {
          assertEquals(12 + i, grid[j * 4 + i], 0.01);
        }
      }
    }
  }

  /** The border is read from the neighbouring tiles, which is the whole reason it is asked for. */
  @Test
  void readsTheBorderFromTheAdjacentTiles() throws Exception {
    var archive = new Archive(1, (x, y) -> x);
    try (var reader = new TerrariumElevationReader(archive, TILE_SIZE, 1)) {
      var grid = reader.read(new TileCoord(1, 1, 2), 4, 2);
      assertEquals(64, grid.length);
      // Four pixels at columns 4 to 7, with two pixels of tile 0 before and two of tile 2 after.
      for (int i = 0; i < 8; i++) {
        assertEquals(2 + i, grid[i], 0.01);
      }
    }
  }

  /** A tile the archive does not hold is sea level, not an error. */
  @Test
  void readsAMissingTileAsSeaLevel() throws Exception {
    TileStore<ByteBuffer> empty = new Archive(1, (x, y) -> 0) {
      @Override
      public ByteBuffer read(TileCoord tileCoord) {
        return null;
      }
    };
    try (var reader = new TerrariumElevationReader(empty, TILE_SIZE, 1)) {
      for (var value : reader.read(new TileCoord(0, 0, 1), 4, 0)) {
        assertEquals(0, value, 0.01);
      }
    }
  }

  /**
   * Past the deepest level the archive holds, the tiles are stretched rather than refused: a map
   * drawn at zoom 16 from an archive that stops at 12 shows smooth terrain, not none.
   */
  @Test
  void samplesBeyondTheDeepestLevelOfTheArchive() throws Exception {
    var archive = new Archive(1, (x, y) -> x);
    try (var reader = new TerrariumElevationReader(archive, TILE_SIZE, 1)) {
      var grid = reader.read(new TileCoord(8, 8, 5), 4, 0);
      assertEquals(16, grid.length);
      // Eight target pixels to one source pixel, so the tile falls between two of them and every
      // value is interpolated rather than repeated.
      for (int i = 0; i < 4; i++) {
        assertEquals(3.5625 + i * 0.125, grid[i], 0.001);
      }
    }
  }

  /** A grid running off the antimeridian continues into the tiles on the other side. */
  @Test
  void wrapsAroundTheAntimeridian() throws Exception {
    var archive = new Archive(1, (x, y) -> x);
    try (var reader = new TerrariumElevationReader(archive, TILE_SIZE, 1)) {
      var grid = reader.read(new TileCoord(0, 1, 2), 4, 2);
      // Two pixels before column 0 are the last two of the world, which is 16 source pixels wide.
      assertEquals(14, grid[0], 0.01);
      assertEquals(15, grid[1], 0.01);
      assertEquals(0, grid[2], 0.01);
    }
  }

  /** A tile of a size the reader was not told about is a mistake worth naming. */
  @Test
  void refusesAnArchiveOfADifferentTileSize() {
    var archive = new Archive(1, (x, y) -> x);
    var reader = new TerrariumElevationReader(archive, TILE_SIZE * 2, 1);
    var exception = assertThrows(TileStoreException.class,
        () -> reader.read(new TileCoord(0, 0, 3), 4, 0));
    assertTrue(exception.getMessage().contains("8x8"), exception.getMessage());
  }
}
