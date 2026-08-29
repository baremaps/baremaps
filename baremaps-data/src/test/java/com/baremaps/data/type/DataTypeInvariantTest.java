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

package com.baremaps.data.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** The invariants of {@link DataType} that collections rely on, over every type and value. */
class DataTypeInvariantTest {

  @ParameterizedTest
  @MethodSource("com.baremaps.data.type.DataTypeProvider#dataTypes")
  @SuppressWarnings({"unchecked", "rawtypes"})
  void sizeIsSelfDelimitingAndNeverZero(DataType dataType, Object value) {
    int size = dataType.size(value);
    assertTrue(size > 0, "size(value) must be positive");

    // Write at an odd offset, surrounded by a sentinel, to catch writes outside the declared size.
    int offset = 13;
    var buffer = ByteBuffer.allocate(offset + size + 16);
    for (int i = 0; i < buffer.capacity(); i++) {
      buffer.put(i, (byte) 0x7f);
    }
    dataType.write(buffer, offset, value);
    assertEquals(size, dataType.size(buffer, offset), "size(buffer) must match size(value)");
    for (int i = 0; i < offset; i++) {
      assertEquals((byte) 0x7f, buffer.get(i), "written before the position");
    }
    for (int i = offset + size; i < buffer.capacity(); i++) {
      assertEquals((byte) 0x7f, buffer.get(i), "written past the declared size");
    }

    // Zero-filled memory must never look like a value of the declared size.
    var zeros = ByteBuffer.allocate(size + 16);
    if (!(dataType instanceof FixedSizeDataType)) {
      assertEquals(0, dataType.size(zeros, 0), "zero-filled memory must have size 0");
    }
  }
}
