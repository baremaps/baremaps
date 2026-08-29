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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.baremaps.data.memory.Memory;
import com.baremaps.data.type.LongDataType;
import com.baremaps.data.type.LongListDataType;
import com.baremaps.data.type.StringDataType;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Collections in memory-mapped storage survive being closed and reopened. */
class PersistenceTest {

  private static final int SEGMENT_BYTES = 1 << 10;

  @Test
  void appendOnlyLog(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("log");
    long[] positions = new long[300];
    try (var log = new AppendOnlyLog<>(new StringDataType(),
        Memory.mappedFile(file, SEGMENT_BYTES))) {
      for (int i = 0; i < 200; i++) {
        positions[i] = log.addPositioned("value-" + i);
      }
    }
    try (var log = new AppendOnlyLog<>(new StringDataType(),
        Memory.mappedFile(file, SEGMENT_BYTES))) {
      assertEquals(200, log.size());
      for (int i = 200; i < 300; i++) {
        positions[i] = log.addPositioned("value-" + i);
      }
    }
    try (var log = new AppendOnlyLog<>(new StringDataType(),
        Memory.mappedFile(file, SEGMENT_BYTES))) {
      assertEquals(300, log.size());
      for (int i = 0; i < 300; i++) {
        assertEquals("value-" + i, log.getPositioned(positions[i]));
      }
      int i = 0;
      for (String value : log) {
        assertEquals("value-" + i++, value);
      }
      assertEquals(300, i);
    }
  }

  @Test
  void fixedSizeDataList(@TempDir Path dir) throws Exception {
    try (var list = new FixedSizeDataList<>(new LongDataType(),
        Memory.mappedDirectory(dir, SEGMENT_BYTES))) {
      for (long i = 0; i < 500; i++) {
        list.add(i);
      }
    }
    try (var list = new FixedSizeDataList<>(new LongDataType(),
        Memory.mappedDirectory(dir, SEGMENT_BYTES))) {
      assertEquals(500, list.size());
      assertEquals(499L, list.get(499));
      assertEquals(500, list.addIndexed(500L));
    }
  }

  @Test
  void sparseDataMap(@TempDir Path dir) throws Exception {
    try (var map = open(dir)) {
      for (long i = 0; i < 500; i++) {
        map.put(i * 3, List.of(i));
      }
    }
    try (var map = open(dir)) {
      assertEquals(500, map.size());
      assertEquals(List.of(499L), map.get(499L * 3));
      assertNull(map.get(1L));
      map.put(1500L, List.of(500L));
      assertEquals(List.of(500L), map.get(1500L));
    }
  }

  @Test
  void denseDataMap(@TempDir Path dir) throws Exception {
    try (var map = openDense(dir)) {
      for (long i = 0; i < 500; i++) {
        map.put(i * 3, i);
      }
    }
    try (var map = openDense(dir)) {
      assertEquals(500, map.size());
      assertEquals(499L, map.get(499L * 3));
      assertNull(map.get(1L));
      assertNull(map.get(1500L));
      // New keys land in new pages after the persisted page count, not over existing ones. The far
      // key stays small: the table is filled contiguously, and 1 KiB segments in a directory map
      // one
      // file per segment.
      map.put(1500L, 500L);
      map.put(1L << 16, 501L);
      assertEquals(500L, map.get(1500L));
      assertEquals(501L, map.get(1L << 16));
      assertEquals(0L, map.get(0L));
      assertEquals(502, map.size());
    }
  }

  @Test
  void unflushedWritesAreNotVisibleAfterReopen(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("log");
    var log = new AppendOnlyLog<>(new StringDataType(), Memory.mappedFile(file, SEGMENT_BYTES));
    log.add("a");
    log.flush();
    log.add("b");
    // Simulate a crash: neither flush nor close.
    try (var reopened = new AppendOnlyLog<>(new StringDataType(),
        Memory.mappedFile(file, SEGMENT_BYTES))) {
      assertEquals(1, reopened.size());
      assertEquals(List.of("a"), reopened.stream().toList());
      // Appending continues after the flushed end and does not clobber "a".
      reopened.add("c");
      assertEquals(List.of("a", "c"), reopened.stream().toList());
    }
  }

  @Test
  void neverClosedIsEmptyOnReopen(@TempDir Path dir) throws Exception {
    var list = new FixedSizeDataList<>(new LongDataType(),
        Memory.mappedDirectory(dir, SEGMENT_BYTES));
    list.add(1L);
    try (var reopened = new FixedSizeDataList<>(new LongDataType(),
        Memory.mappedDirectory(dir, SEGMENT_BYTES))) {
      assertEquals(0, reopened.size());
    }
  }

  private static DenseDataMap<Long> openDense(Path dir) {
    return new DenseDataMap<>(new LongDataType(), 6,
        Memory.mappedDirectory(dir.resolve("table"), SEGMENT_BYTES),
        Memory.mappedDirectory(dir.resolve("presence"), SEGMENT_BYTES),
        Memory.mappedDirectory(dir.resolve("values"), SEGMENT_BYTES));
  }

  private static SparseDataMap<List<Long>> open(Path dir) {
    return new SparseDataMap<>(
        new FixedSizeDataList<>(new LongDataType(),
            Memory.mappedDirectory(dir.resolve("offsets"), SEGMENT_BYTES)),
        new FixedSizeDataList<>(new LongDataType(),
            Memory.mappedDirectory(dir.resolve("keys"), SEGMENT_BYTES)),
        new VariableSizeDataList<>(
            new FixedSizeDataList<>(new LongDataType(),
                Memory.mappedDirectory(dir.resolve("index"), SEGMENT_BYTES)),
            new AppendOnlyLog<>(new LongListDataType(),
                Memory.mappedDirectory(dir.resolve("values"), SEGMENT_BYTES))));
  }
}
