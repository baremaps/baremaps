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

package com.baremaps.tilestore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.math.IntMath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class TileBatcherTest {

  @Test
  void testFiltering() {
    // Compute all the tiles for zoom levels 0 to 3
    final int streamSize = 85;
    final List<TileCoord> tileCoords = new ArrayList<>();
    for (int z = 0; z <= 3; z++) {
      for (int x = 0; x < IntMath.pow(2, z); x++) {
        for (int y = 0; y < IntMath.pow(2, z); y++) {
          tileCoords.add(new TileCoord(x, y, z));
        }
      }
    }
    assertEquals(streamSize, tileCoords.size());

    // ensures that the batches have the correct size and retain de correct tiles
    final int batchArraySize = 5;
    for (int batchArrayIndex = 0; batchArrayIndex < batchArraySize; batchArrayIndex++) {
      List<TileCoord> batch =
          tileCoords.stream().filter(new TileBatchPredicate(batchArraySize, batchArrayIndex))
              .sorted(Comparator.comparingLong(TileCoord::index)).toList();
      assertEquals(streamSize / batchArraySize, batch.size());
      int tileIndex = batchArrayIndex;
      for (TileCoord tileCoord : batch) {
        assertEquals(tileIndex, tileCoord.index());
        tileIndex += batchArraySize;
      }
    }
  }
}
