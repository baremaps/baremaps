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

import com.baremaps.data.memory.MemoryMappedDirectory;
import com.baremaps.data.memory.MemoryMappedFile;
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
        new MemoryMappedFile(file, SEGMENT_BYTES))) {
      for (int i = 0; i < 200; i++) {
        positions[i] = log.addPositioned("value-" + i);
      }
    }
    try (var log = new AppendOnlyLog<>(new StringDataType(),
        new MemoryMappedFile(file, SEGMENT_BYTES))) {
      assertEquals(200, log.size());
      for (int i = 200; i < 300; i++) {
        positions[i] = log.addPositioned("value-" + i);
      }
    }
    try (var log = new AppendOnlyLog<>(new StringDataType(),
        new MemoryMappedFile(file, SEGMENT_BYTES))) {
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
  void memoryAlignedDataList(@TempDir Path dir) throws Exception {
    try (var list = new MemoryAlignedDataList<>(new LongDataType(),
        new MemoryMappedDirectory(dir, SEGMENT_BYTES))) {
      for (long i = 0; i < 500; i++) {
        list.add(i);
      }
    }
    try (var list = new MemoryAlignedDataList<>(new LongDataType(),
        new MemoryMappedDirectory(dir, SEGMENT_BYTES))) {
      assertEquals(500, list.size());
      assertEquals(499L, list.get(499));
      assertEquals(500, list.addIndexed(500L));
    }
  }

  @Test
  void fixedSizeDataList(@TempDir Path dir) throws Exception {
    try (var list = new FixedSizeDataList<>(new LongDataType(),
        new MemoryMappedDirectory(dir, SEGMENT_BYTES))) {
      for (long i = 0; i < 500; i++) {
        list.add(i);
      }
    }
    try (var list = new FixedSizeDataList<>(new LongDataType(),
        new MemoryMappedDirectory(dir, SEGMENT_BYTES))) {
      assertEquals(500, list.size());
      assertEquals(499L, list.get(499));
    }
  }

  @Test
  void monotonicDataMap(@TempDir Path dir) throws Exception {
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

  private static MonotonicDataMap<List<Long>> open(Path dir) {
    return new MonotonicDataMap<>(
        new MemoryAlignedDataList<>(new LongDataType(),
            new MemoryMappedDirectory(dir.resolve("offsets"), SEGMENT_BYTES)),
        new MemoryAlignedDataList<>(new LongDataType(),
            new MemoryMappedDirectory(dir.resolve("keys"), SEGMENT_BYTES)),
        new IndexedDataList<>(
            new MemoryAlignedDataList<>(new LongDataType(),
                new MemoryMappedDirectory(dir.resolve("index"), SEGMENT_BYTES)),
            new AppendOnlyLog<>(new LongListDataType(),
                new MemoryMappedDirectory(dir.resolve("values"), SEGMENT_BYTES))));
  }
}
