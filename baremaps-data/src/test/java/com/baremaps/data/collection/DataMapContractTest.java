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
import com.baremaps.data.type.LongDataType;
import com.baremaps.data.type.LongListDataType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The contract every {@link DataMap} must honour.
 */
class DataMapContractTest {

  private static final int SEGMENT_BYTES = 1 << 10;

  static Stream<Arguments> maps() {
    return Stream.of(
        Arguments.of(new SparseDataMap<>(
            new FixedSizeDataList<>(new LongDataType(), Memory.offHeap(SEGMENT_BYTES)),
            new FixedSizeDataList<>(new LongDataType(), Memory.offHeap(SEGMENT_BYTES)),
            new VariableSizeDataList<>(
                new FixedSizeDataList<>(new LongDataType(), Memory.offHeap(SEGMENT_BYTES)),
                new AppendOnlyLog<>(new LongListDataType(), Memory.offHeap(SEGMENT_BYTES))))),
        Arguments.of(new VariableSizeDataMap<>(
            new DenseDataMap<>(new LongDataType(), 6, Memory.offHeap(SEGMENT_BYTES),
                Memory.offHeap(SEGMENT_BYTES), Memory.offHeap(SEGMENT_BYTES)),
            new AppendOnlyLog<>(new LongListDataType(), Memory.offHeap(SEGMENT_BYTES)))));
  }

  // Sparse, increasing keys with gaps larger than a chunk.
  private static long key(long i) {
    return i * 300;
  }

  private static List<Long> value(long i) {
    return List.of(i, i + 1);
  }

  @ParameterizedTest
  @MethodSource("maps")
  void putGet(DataMap<Long, List<Long>> map) throws Exception {
    assertTrue(map.isEmpty());
    for (long i = 0; i < 1000; i++) {
      map.put(key(i), value(i));
    }
    assertEquals(1000, map.size());
    for (long i = 0; i < 1000; i++) {
      assertEquals(value(i), map.get(key(i)));
      assertTrue(map.containsKey(key(i)));
    }
    map.close();
  }

  @ParameterizedTest
  @MethodSource("maps")
  void absentKeys(DataMap<Long, List<Long>> map) throws Exception {
    for (long i = 0; i < 100; i++) {
      map.put(key(i), value(i));
    }
    assertNull(map.get(-1L));
    assertNull(map.get(1L));
    assertNull(map.get(key(100)));
    assertNull(map.get(Long.MAX_VALUE));
    assertNull(map.get("not a long"));
    assertNull(map.get(null));
    assertFalse(map.containsKey(1L));
    assertFalse(map.containsKey("not a long"));
    assertFalse(map.containsKey(null));
    assertFalse(map.containsValue(value(100)));
    assertTrue(map.containsValue(value(50)));
    map.close();
  }

  @ParameterizedTest
  @MethodSource("maps")
  void iterators(DataMap<Long, List<Long>> map) throws Exception {
    var expected = new HashMap<Long, List<Long>>();
    for (long i = 0; i < 100; i++) {
      map.put(key(i), value(i));
      expected.put(key(i), value(i));
    }
    var entries = new HashMap<Long, List<Long>>();
    map.forEach(entries::put);
    assertEquals(expected, entries);
    var keys = new java.util.HashSet<Long>();
    map.keys().forEach(keys::add);
    assertEquals(expected.keySet(), keys);
    var values = new java.util.HashSet<List<Long>>();
    map.values().forEach(values::add);
    assertEquals(new java.util.HashSet<>(expected.values()), values);
    map.close();
  }

  @ParameterizedTest
  @MethodSource("maps")
  void clearThenReuse(DataMap<Long, List<Long>> map) throws Exception {
    for (long i = 0; i < 100; i++) {
      map.put(key(i), value(i));
    }
    map.clear();
    assertTrue(map.isEmpty());
    assertNull(map.get(key(0)));
    for (long i = 0; i < 100; i++) {
      map.put(key(i), value(i + 1));
    }
    assertEquals(value(1), map.get(key(0)));
    map.close();
  }

  @Test
  void sparseKeysMustIncrease() {
    var map = new SparseDataMap<>(new LongDataType());
    map.put(10L, 1L);
    assertThrows(IllegalArgumentException.class, () -> map.put(10L, 2L));
    assertThrows(IllegalArgumentException.class, () -> map.put(5L, 2L));
    map.put(11L, 3L);
    assertEquals(Map.of(10L, 1L, 11L, 3L), DataConversions.asMap(map));
  }

  @Test
  void sparseGetAllWalksSortedKeys() {
    var map = new SparseDataMap<>(new LongDataType());
    for (long i = 0; i < 2000; i++) {
      map.put(i * 3, i);
    }
    // Increasing keys within and across chunks, with misses in between, then a backwards key.
    var keys = List.of(0L, 3L, 4L, 6L, 300L, 303L, 765L, 768L, 6000L, 5997L, 1L, 5994L);
    var expected = new java.util.ArrayList<Long>();
    for (long key : keys) {
      expected.add(key % 3 == 0 && key < 6000 ? key / 3 : null);
    }
    assertEquals(expected, map.getAll(keys));
    assertEquals(map.get(5997L), map.get((Object) 5997L));
  }
}
