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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DataTypeTest {

  @ParameterizedTest
  @MethodSource("com.baremaps.data.type.DataTypeProvider#dataTypes")
  void writeAndRead(DataType dataType, Object value) {
    var size = dataType.size(value);
    var buffer = ByteBuffer.allocate(size);
    dataType.write(buffer, 0, value);
    var recordSize = dataType.size(buffer, 0);
    var recordValue = dataType.read(buffer, 0);

    assertEquals(size, recordSize);

    if (value instanceof byte[]) {
      assertArrayEquals((byte[]) value, (byte[]) recordValue);
    } else if (value instanceof short[]) {
      assertArrayEquals((short[]) value, (short[]) recordValue);
    } else if (value instanceof int[]) {
      assertArrayEquals((int[]) value, (int[]) recordValue);
    } else if (value instanceof long[]) {
      assertArrayEquals((long[]) value, (long[]) recordValue);
    } else if (value instanceof float[]) {
      assertArrayEquals((float[]) value, (float[]) recordValue);
    } else if (value instanceof double[]) {
      assertArrayEquals((double[]) value, (double[]) recordValue);
    } else if (value instanceof char[]) {
      assertArrayEquals((char[]) value, (char[]) recordValue);
    } else if (value instanceof boolean[]) {
      assertArrayEquals((boolean[]) value, (boolean[]) recordValue);
    } else {
      assertEquals(value, recordValue);
    }
  }
}
