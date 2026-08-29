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

package com.baremaps.data.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.data.memory.Memory;
import com.baremaps.data.type.IntegerDataType;
import com.baremaps.data.type.LongDataType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** The behaviours of {@link DenseDataMap} that the contract tests do not reach. */
class DenseDataMapTest {

  private static final int SEGMENT = 1 << 10;

  private static DenseDataMap<Long> create() {
    // Pages of 64 keys, so that a few hundred keys span many pages and many segments.
    return new DenseDataMap<>(new LongDataType(), 6, Memory.offHeap(SEGMENT),
        Memory.offHeap(SEGMENT), Memory.offHeap(SEGMENT));
  }

  @Test
  void zeroValuesAreDistinctFromAbsentKeys() throws Exception {
    try (var map = create()) {
      assertNull(map.get(0L));
      assertFalse(map.containsKey(0L));
      assertNull(map.put(0L, 0L));
      assertEquals(0L, map.get(0L));
      assertTrue(map.containsKey(0L));
      // The neighbour shares the same page and bitmap word but is still absent.
      assertNull(map.get(1L));
      assertFalse(map.containsKey(1L));
      assertEquals(1, map.size());
    }
  }

  @Test
  void putReturnsPreviousAndKeepsSize() throws Exception {
    try (var map = create()) {
      assertNull(map.put(70L, 1L));
      assertEquals(1L, map.put(70L, 2L));
      assertEquals(2L, map.get(70L));
      assertEquals(1, map.size());
    }
  }

  @Test
  void keysInAnyOrderAcrossPagesAndSegments() throws Exception {
    var expected = new TreeMap<Long, Long>();
    var random = new Random(42);
    try (var map = create()) {
      for (int i = 0; i < 5000; i++) {
        long key = random.nextLong(1L << 20);
        map.put(key, (long) i);
        expected.put(key, (long) i);
      }
      assertEquals(expected.size(), map.size());
      for (var entry : expected.entrySet()) {
        assertEquals(entry.getValue(), map.get(entry.getKey()));
      }
      // Iteration is in key order and skips the unallocated pages and the empty slots.
      var keys = new ArrayList<Long>();
      map.keys().forEach(keys::add);
      assertEquals(new ArrayList<>(expected.keySet()), keys);
      var entries = new TreeMap<Long, Long>();
      map.forEach(entries::put);
      assertEquals(expected, entries);
    }
  }

  @Test
  void iteratorOverPageAndWordBoundaries() throws Exception {
    try (var map = create()) {
      // First and last slot of a page, both ends of a bitmap word, and a far-away page.
      var keys = List.of(0L, 63L, 64L, 127L, 128L, 191L, 1000L, 100_000L);
      for (long key : keys) {
        map.put(key, key);
      }
      var seen = new ArrayList<Long>();
      map.keys().forEach(seen::add);
      assertEquals(keys, seen);
      assertNull(map.get(65L));
      assertNull(map.get(1001L));
      assertNull(map.get(1L << 40));
    }
  }

  @Test
  void rejectsBadKeysAndTypes() throws Exception {
    try (var map = create()) {
      assertThrows(IndexOutOfBoundsException.class, () -> map.put(-1L, 1L));
      assertThrows(IndexOutOfBoundsException.class, () -> map.put(Long.MAX_VALUE, 1L));
      assertThrows(NullPointerException.class, () -> map.put(null, 1L));
      assertThrows(NullPointerException.class, () -> map.put(1L, null));
      assertNull(map.get(-1L));
      assertNull(map.get("not a long"));
      assertNull(map.get(null));
    }
    assertThrows(DataCollectionException.class, () -> new DenseDataMap<>(new IntegerDataType(), 5,
        Memory.offHeap(SEGMENT), Memory.offHeap(SEGMENT), Memory.offHeap(SEGMENT)));
    assertThrows(DataCollectionException.class, () -> new DenseDataMap<>(new LongDataType(), 10,
        Memory.offHeap(SEGMENT), Memory.offHeap(SEGMENT), Memory.offHeap(SEGMENT)));
  }

  @Test
  void asJavaMap() throws Exception {
    try (var map = create()) {
      Map<Long, Long> view = DataConversions.asMap(map);
      view.put(5L, 50L);
      view.put(500L, 5000L);
      assertEquals(Map.of(5L, 50L, 500L, 5000L), view);
    }
  }
}
