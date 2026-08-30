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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    assertEquals(new TileCoord(0, 0, 0), TileIdConverter.tileIdToZxy(0));
    assertEquals(new TileCoord(1, 0, 0), TileIdConverter.tileIdToZxy(1));
    assertEquals(new TileCoord(1, 0, 1), TileIdConverter.tileIdToZxy(2));
    assertEquals(new TileCoord(1, 1, 1), TileIdConverter.tileIdToZxy(3));
    assertEquals(new TileCoord(1, 1, 0), TileIdConverter.tileIdToZxy(4));
    assertEquals(new TileCoord(2, 0, 0), TileIdConverter.tileIdToZxy(5));
  }

  @Test
  void roundTripEveryTileUpToZoomEight() {
    for (var z = 0; z < 9; z++) {
      for (var x = 0L; x < 1L << z; x++) {
        for (var y = 0L; y < 1L << z; y++) {
          assertEquals(new TileCoord(z, x, y),
              TileIdConverter.tileIdToZxy(TileIdConverter.zxyToTileId(z, x, y)));
        }
      }
    }
  }

  @Test
  void roundTripCornersOfEveryZoom() {
    for (var z = 0; z <= 26; z++) {
      var last = (1L << z) - 1;
      for (var corner : new long[][] {{0, 0}, {last, 0}, {0, last}, {last, last}}) {
        var coord = new TileCoord(z, corner[0], corner[1]);
        assertEquals(coord,
            TileIdConverter.tileIdToZxy(TileIdConverter.zxyToTileId(z, coord.x(), coord.y())));
      }
    }
  }

  @Test
  void rejectCoordinatesOutsideTheirZoom() {
    assertThrows(IllegalArgumentException.class, () -> TileIdConverter.zxyToTileId(27, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> TileIdConverter.zxyToTileId(-1, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> TileIdConverter.zxyToTileId(0, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> TileIdConverter.zxyToTileId(1, -1, 0));
  }

  @Test
  void rejectTileIdsBeyondTheLastZoom() {
    assertThrows(IllegalArgumentException.class, () -> TileIdConverter.tileIdToZxy(-1));
    assertThrows(IllegalArgumentException.class,
        () -> TileIdConverter.tileIdToZxy(9007199254740991L));
  }
}
