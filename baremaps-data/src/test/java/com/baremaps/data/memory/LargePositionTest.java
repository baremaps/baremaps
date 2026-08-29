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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.baremaps.data.type.LongDataType;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Positions beyond {@link Integer#MAX_VALUE}. Memory-mapped files are sparse, so mapping gigabyte
 * segments costs nothing until a page is touched.
 */
class LargePositionTest {

  private static final int SEGMENT = 1 << 30;

  @Test
  void positionsBeyondIntRange(@TempDir Path dir) throws Exception {
    var type = new LongDataType();
    try (var memory = Memory.mappedDirectory(dir, SEGMENT)) {
      long[] positions = {
          Integer.MAX_VALUE - 7L, // last long of segment 1
          (long) Integer.MAX_VALUE + 1, // first byte of segment 2
          5L * SEGMENT + 12345 * 8L, // deep into segment 5
          7L * SEGMENT - 8L}; // last long of segment 6
      for (long position : positions) {
        memory.write(type, position, position);
      }
      for (long position : positions) {
        assertEquals(position, memory.read(type, position));
      }
      assertEquals(7, memory.segmentCount());
      assertEquals(7L * SEGMENT, memory.size());
      memory.clear();
    }
  }
}
