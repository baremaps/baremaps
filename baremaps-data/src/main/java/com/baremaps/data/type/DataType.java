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
 * A data type for reading and writing values in {@link ByteBuffer}s. Read and write operations must
 * use absolute positions within the {@link ByteBuffer}.
 *
 * @param <T> the type of value being read or written
 */
public interface DataType<T> {

  /**
   * Returns the size of the value.
   *
   * @param value the value
   * @return the size of the value in bytes
   */
  int size(final T value);

  /**
   * Returns the size of the value stored at the specified position in a {@link ByteBuffer}.
   *
   * @param buffer the buffer containing the value
   * @param position the absolute position of the value within the buffer
   * @return the size of the value in bytes
   */
  int size(final ByteBuffer buffer, final int position);

  /**
   * Writes a value to the specified position in a {@link ByteBuffer}.
   *
   * @param buffer the destination buffer
   * @param position the absolute position within the buffer to write the value
   * @param value the value to write
   */
  void write(final ByteBuffer buffer, final int position, final T value);

  /**
   * Reads a value from the specified position in a {@link ByteBuffer}.
   *
   * @param buffer the source buffer
   * @param position the absolute position within the buffer to read the value
   * @return the read value
   */
  T read(final ByteBuffer buffer, final int position);

}
