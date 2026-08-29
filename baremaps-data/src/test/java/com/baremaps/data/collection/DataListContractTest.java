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

import static com.baremaps.data.memory.MemoryProvider.SEGMENT_BYTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.data.memory.Memory;
import com.baremaps.data.type.LongDataType;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** The contract every {@link DataList} must honour, over every memory. */
class DataListContractTest {

  // Enough elements to span several small segments.
  private static final int N = 1000;

  static Stream<Arguments> lists() throws IOException {
    List<Function<Supplier<Memory>, DataList<Long>>> factories = List.of(
        m -> new FixedSizeDataList<>(new LongDataType(), m.get()),
        m -> new MemoryAlignedDataList<>(new LongDataType(), m.get()),
        m -> new IndexedDataList<>(
            new MemoryAlignedDataList<>(new LongDataType(), m.get()),
            new AppendOnlyLog<>(new LongDataType(), m.get())));
    List<Arguments> arguments = new ArrayList<>();
    for (var factory : factories) {
      for (var memory : memories()) {
        arguments.add(Arguments.of(factory.apply(memory)));
      }
    }
    return arguments.stream();
  }

  private static List<Supplier<Memory>> memories() {
    return List.of(
        () -> Memory.offHeap(SEGMENT_BYTES),
        () -> Memory.mappedFile(tempFile(), SEGMENT_BYTES),
        () -> Memory.mappedDirectory(tempDirectory(), SEGMENT_BYTES));
  }

  private static java.nio.file.Path tempFile() {
    try {
      return Files.createTempFile("baremaps_", ".tmp");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static java.nio.file.Path tempDirectory() {
    try {
      return Files.createTempDirectory("baremaps_");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @ParameterizedTest
  @MethodSource("lists")
  void addGetSet(DataList<Long> list) throws Exception {
    assertTrue(list.isEmpty());
    for (long i = 0; i < N; i++) {
      assertEquals(i, list.addIndexed(i));
    }
    assertEquals(N, list.size());
    for (long i = 0; i < N; i++) {
      assertEquals(i, list.get(i));
    }
    for (long i = 0; i < N; i++) {
      list.set(i, i + 1);
    }
    for (long i = 0; i < N; i++) {
      assertEquals(i + 1, list.get(i));
    }
    list.close();
  }

  @ParameterizedTest
  @MethodSource("lists")
  void bounds(DataList<Long> list) throws Exception {
    assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
    list.add(1L);
    assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
    assertThrows(IndexOutOfBoundsException.class, () -> list.set(1, 1L));
    list.close();
  }

  @ParameterizedTest
  @MethodSource("lists")
  void iterate(DataList<Long> list) throws Exception {
    for (long i = 0; i < N; i++) {
      list.add(i);
    }
    long expected = 0;
    for (long value : list) {
      assertEquals(expected++, value);
    }
    assertEquals(N, expected);
    assertEquals(N, list.stream().count());
    assertTrue(list.contains(N - 1L));
    assertFalse(list.contains((long) N));
    list.close();
  }

  @ParameterizedTest
  @MethodSource("lists")
  void clearThenReuse(DataList<Long> list) throws Exception {
    for (long i = 0; i < N; i++) {
      list.add(i);
    }
    list.clear();
    assertEquals(0, list.size());
    assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
    for (long i = 0; i < N; i++) {
      assertEquals(i, list.addIndexed(i * 2));
    }
    for (long i = 0; i < N; i++) {
      assertEquals(i * 2, list.get(i));
    }
    list.close();
  }
}
