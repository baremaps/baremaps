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

import java.nio.ByteBuffer;

/**
 * A {@link DataType} for reading and writing arrays of integer values in {@link ByteBuffer}s.
 */
public class IntegerArrayDataType implements DataType<int[]> {

  /** {@inheritDoc} */
  @Override
  public int size(final int[] values) {
    return Integer.BYTES + values.length * Integer.BYTES;
  }

  /** {@inheritDoc} */
  @Override
  public int size(final ByteBuffer buffer, final int position) {
    return buffer.getInt(position);
  }

  /** {@inheritDoc} */
  @Override
  public void write(final ByteBuffer buffer, final int position, final int[] values) {
    buffer.putInt(position, size(values));
    var p = position + Integer.BYTES;
    for (int value : values) {
      buffer.putInt(p, value);
      p += Integer.BYTES;
    }
  }

  /** {@inheritDoc} */
  @Override
  public int[] read(final ByteBuffer buffer, final int position) {
    int size = buffer.getInt(position);
    int length = (size - Integer.BYTES) / Integer.BYTES;
    int[] values = new int[length];
    for (int index = 0; index < length; index++) {
      values[index] = buffer.getInt(position + Integer.BYTES + index * Integer.BYTES);
    }
    return values;
  }
}
