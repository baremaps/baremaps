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

import java.lang.foreign.MemorySegment;

/**
 * A data type for reading and writing values at absolute positions in {@link MemorySegment}s.
 *
 * <p>
 * Values are never null. An encoding is self-delimiting: {@link #size(MemorySegment, long)}
 * recovers the size of a value from its first bytes and is never 0 for a written value, so that
 * collections can tell written space from zero-filled space.
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
   * Returns the size of the value stored at the specified position in a {@link MemorySegment}.
   *
   * @param segment the segment containing the value
   * @param position the absolute position of the value within the segment
   * @return the size of the value in bytes
   */
  int size(final MemorySegment segment, final long position);

  /**
   * Writes a value to the specified position in a {@link MemorySegment}.
   *
   * @param segment the destination segment
   * @param position the absolute position within the segment to write the value
   * @param value the value to write
   */
  void write(final MemorySegment segment, final long position, final T value);

  /**
   * Reads a value from the specified position in a {@link MemorySegment}.
   *
   * @param segment the source segment
   * @param position the absolute position within the segment to read the value
   * @return the read value
   */
  T read(final MemorySegment segment, final long position);

}
