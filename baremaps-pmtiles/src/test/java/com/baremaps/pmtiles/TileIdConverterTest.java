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

package com.baremaps.pmtiles;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.math.LongMath;
import org.junit.jupiter.api.Test;

/**
 * Tests for the TileIdConverter class.
 */
class TileIdConverterTest {

  @Test
  void zxyToTileId() {
    assertEquals(0, TileIdConverter.zxyToTileId(0, 0, 0));
    assertEquals(1, TileIdConverter.zxyToTileId(1, 0, 0));
    assertEquals(2, TileIdConverter.zxyToTileId(1, 0, 1));
    assertEquals(3, TileIdConverter.zxyToTileId(1, 1, 1));
    assertEquals(4, TileIdConverter.zxyToTileId(1, 1, 0));
    assertEquals(5, TileIdConverter.zxyToTileId(2, 0, 0));
  }

  @Test
  void tileIdToZxy() {
    assertArrayEquals(new long[] {0, 0, 0}, TileIdConverter.tileIdToZxy(0));
    assertArrayEquals(new long[] {1, 0, 0}, TileIdConverter.tileIdToZxy(1));
    assertArrayEquals(new long[] {1, 0, 1}, TileIdConverter.tileIdToZxy(2));
    assertArrayEquals(new long[] {1, 1, 1}, TileIdConverter.tileIdToZxy(3));
    assertArrayEquals(new long[] {1, 1, 0}, TileIdConverter.tileIdToZxy(4));
    assertArrayEquals(new long[] {2, 0, 0}, TileIdConverter.tileIdToZxy(5));
  }

  @Test
  void aLotOfTiles() {
    for (int z = 0; z < 9; z++) {
      for (long x = 0; x < 1 << z; x++) {
        for (long y = 0; y < 1 << z; y++) {
          var result = TileIdConverter.tileIdToZxy(TileIdConverter.zxyToTileId(z, x, y));
          if (result[0] != z || result[1] != x || result[2] != y) {
            fail("roundtrip failed");
          }
        }
      }
    }
  }

  @Test
  void tileExtremes() {
    for (var z = 0; z < 27; z++) {
      var dim = LongMath.pow(2, z) - 1;
      var tl = TileIdConverter.tileIdToZxy(TileIdConverter.zxyToTileId(z, 0, 0));
      assertArrayEquals(new long[] {z, 0, 0}, tl);
      var tr = TileIdConverter.tileIdToZxy(TileIdConverter.zxyToTileId(z, dim, 0));
      assertArrayEquals(new long[] {z, dim, 0}, tr);
      var bl = TileIdConverter.tileIdToZxy(TileIdConverter.zxyToTileId(z, 0, dim));
      assertArrayEquals(new long[] {z, 0, dim}, bl);
      var br = TileIdConverter.tileIdToZxy(TileIdConverter.zxyToTileId(z, dim, dim));
      assertArrayEquals(new long[] {z, dim, dim}, br);
    }
  }

  @Test
  void invalidTiles() {
    assertThrows(RuntimeException.class, () -> TileIdConverter.tileIdToZxy(9007199254740991L));
    assertThrows(RuntimeException.class, () -> TileIdConverter.zxyToTileId(27, 0, 0));
    assertThrows(RuntimeException.class, () -> TileIdConverter.zxyToTileId(0, 1, 1));
  }
}
