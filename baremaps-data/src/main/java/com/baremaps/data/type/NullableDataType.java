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
 * A {@link DataType} for reading and writing nullable values in {@link ByteBuffer}s.
 *
 * @param <T> the type of value being read or written
 */
public class NullableDataType<T> implements DataType<T> {

  private final DataType<T> dataType;

  /**
   * Constructs a {@link NullableDataType} with a data type for the wrapped value.
   *
   * @param dataType the data type for the non-null values
   */
  public NullableDataType(final DataType<T> dataType) {
    this.dataType = dataType;
  }

  /** {@inheritDoc} */
  @Override
  public int size(final T value) {
    return Byte.BYTES + dataType.size(value);
  }

  /** {@inheritDoc} */
  @Override
  public int size(final ByteBuffer buffer, final int position) {
    return Byte.BYTES + dataType.size(buffer, position + 1);
  }

  /** {@inheritDoc} */
  @Override
  public void write(final ByteBuffer buffer, final int position, final T value) {
    if (value == null) {
      buffer.put(position, (byte) 0);
    } else {
      buffer.put(position, (byte) 1);
      dataType.write(buffer, position + Byte.BYTES, value);
    }
  }

  /** {@inheritDoc} */
  @Override
  public T read(final ByteBuffer buffer, final int position) {
    if (buffer.get(position) == 0) {
      return null;
    } else {
      return dataType.read(buffer, position + Byte.BYTES);
    }
  }
}
