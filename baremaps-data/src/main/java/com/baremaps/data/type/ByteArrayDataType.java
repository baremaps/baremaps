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
 * A {@link DataType} for reading and writing byte arrays in {@link ByteBuffer}s.
 */
public class ByteArrayDataType implements DataType<byte[]> {

  /**
   * {@inheritDoc}
   */
  @Override
  public int size(final byte[] values) {
    return Integer.BYTES + values.length * Byte.BYTES;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size(final ByteBuffer buffer, final int position) {
    return buffer.getInt(position);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final ByteBuffer buffer, final int position, final byte[] values) {
    buffer.putInt(position, size(values));
    buffer.put(position + Integer.BYTES, values, 0, values.length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] read(final ByteBuffer buffer, final int position) {
    int size = buffer.getInt(position);
    byte[] values = new byte[Math.max(size - Integer.BYTES, 0)];
    buffer.get(position + Integer.BYTES, values);
    return values;
  }
}
