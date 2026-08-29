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

package com.baremaps.data.memory;

import static com.baremaps.data.memory.MemoryProvider.SEGMENT_BYTES;
import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.data.type.IntegerListDataType;
import com.baremaps.data.type.LongDataType;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MemoryTest {

  private static final int SEGMENT_NUMBER = 10;

  @ParameterizedTest
  @MethodSource("com.baremaps.data.memory.MemoryProvider#memories")
  void segment(Memory memory) throws IOException {
    assertEquals(SEGMENT_BYTES, memory.segmentSize());
    for (int i = 0; i < SEGMENT_NUMBER; i++) {
      assertEquals(SEGMENT_BYTES, memory.segment(i).byteSize());
      assertSame(memory.segment(i), memory.segment(i));
      assertNotSame(memory.segment(i), memory.segment(i + 1));
    }
    assertEquals(SEGMENT_NUMBER + 1, memory.segmentCount());
    assertEquals((long) (SEGMENT_NUMBER + 1) * SEGMENT_BYTES, memory.size());
    memory.clear();
    memory.close();
  }

  @ParameterizedTest
  @MethodSource("com.baremaps.data.memory.MemoryProvider#memories")
  void readWrite(Memory memory) throws IOException {
    var type = new LongDataType();
    for (long i = 0; i < 1000; i++) {
      memory.write(type, i * Long.BYTES, i);
    }
    for (long i = 0; i < 1000; i++) {
      assertEquals(i, memory.read(type, i * Long.BYTES));
    }
    memory.clear();
    memory.close();
  }

  @ParameterizedTest
  @MethodSource("com.baremaps.data.memory.MemoryProvider#memories")
  void bounds(Memory memory) throws IOException {
    var type = new LongDataType();
    assertThrows(IndexOutOfBoundsException.class, () -> memory.read(type, -1));
    assertThrows(IndexOutOfBoundsException.class, () -> memory.write(type, -1, 1L));
    assertThrows(IndexOutOfBoundsException.class, () -> memory.write(type, SEGMENT_BYTES - 4, 1L));
    assertThrows(IndexOutOfBoundsException.class,
        () -> memory.read(type, ((long) Integer.MAX_VALUE + 1) * SEGMENT_BYTES));
    memory.clear();
    memory.close();
  }

  @ParameterizedTest
  @MethodSource("com.baremaps.data.memory.MemoryProvider#memories")
  void sizeOf(Memory memory) throws IOException {
    var type = new IntegerListDataType();
    var value = List.of(1, 2, 3);
    memory.write(type, 0, value);
    assertEquals(type.size(value), memory.sizeOf(type, 0));
    // Never written: zero-filled.
    assertEquals(0, memory.sizeOf(type, 100));
    // Too close to the end of the segment to hold a size.
    assertEquals(0, memory.sizeOf(type, SEGMENT_BYTES - 2));
    memory.clear();
    memory.close();
  }

  @ParameterizedTest
  @MethodSource("com.baremaps.data.memory.MemoryProvider#memories")
  void header(Memory memory) throws IOException {
    assertEquals(Memory.HEADER_BYTES, memory.header().byteSize());
    memory.header().set(JAVA_LONG_UNALIGNED, 0, 42L);
    assertEquals(42L, memory.header().get(JAVA_LONG_UNALIGNED, 0));
    memory.clear();
    memory.close();
  }

  @ParameterizedTest
  @MethodSource("com.baremaps.data.memory.MemoryProvider#memories")
  void clearThenReuse(Memory memory) throws IOException {
    var type = new LongDataType();
    memory.write(type, 0, 1L);
    memory.header().set(JAVA_LONG_UNALIGNED, 0, 1L);
    memory.clear();
    assertEquals(0, memory.segmentCount());
    assertEquals(0L, memory.header().get(JAVA_LONG_UNALIGNED, 0));
    assertEquals(0L, memory.read(type, 0));
    memory.clear();
    memory.close();
  }

  @ParameterizedTest
  @MethodSource("com.baremaps.data.memory.MemoryProvider#memories")
  void closed(Memory memory) throws IOException {
    memory.segment(0);
    memory.close();
    memory.close();
    assertTrue(memory.isClosed());
    assertThrows(IllegalStateException.class, () -> memory.segment(1));
    assertThrows(IllegalStateException.class, () -> memory.header().get(JAVA_LONG_UNALIGNED, 0));
    memory.clear();
  }

  @Test
  void segmentSizeMustBePowerOfTwo() {
    assertThrows(IllegalArgumentException.class, () -> Memory.offHeap(1000));
    assertThrows(IllegalArgumentException.class, () -> Memory.offHeap(0));
  }
}
