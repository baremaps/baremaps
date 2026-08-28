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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MemoryTest {

  private static final int SEGMENT_NUMBER = 10;

  @ParameterizedTest
  @MethodSource("com.baremaps.data.memory.MemoryProvider#memories")
  void capacity(Memory memory) throws IOException {
    assertEquals(SEGMENT_BYTES, memory.segmentSize());
    memory.close();
    memory.clear();
  }

  @ParameterizedTest
  @MethodSource("com.baremaps.data.memory.MemoryProvider#memories")
  void segment(Memory memory) throws IOException {
    for (int i = 0; i < SEGMENT_NUMBER; i++) {
      assertEquals(SEGMENT_BYTES, memory.segment(i).capacity());
      assertSame(memory.segment(i), memory.segment(i));
      assertNotSame(memory.segment(i), memory.segment(i + 1));
    }
    memory.close();
    memory.clear();
  }
}
