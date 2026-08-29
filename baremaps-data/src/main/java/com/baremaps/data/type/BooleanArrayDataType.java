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

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;

import java.lang.foreign.MemorySegment;

/**
 * A {@link DataType} for reading and writing arrays of boolean values in {@link MemorySegment}s.
 */
public class BooleanArrayDataType implements DataType<boolean[]> {

  /** {@inheritDoc} */
  @Override
  public int size(final boolean[] values) {
    return Integer.BYTES + values.length * Byte.BYTES;
  }

  /** {@inheritDoc} */
  @Override
  public int size(final MemorySegment segment, final long position) {
    return segment.get(JAVA_INT_UNALIGNED, position);
  }

  /** {@inheritDoc} */
  @Override
  public void write(final MemorySegment segment, final long position, final boolean[] values) {
    segment.set(JAVA_INT_UNALIGNED, position, size(values));
    long p = position + Integer.BYTES;
    for (boolean value : values) {
      segment.set(JAVA_BYTE, p, (byte) (value ? 1 : 0));
      p += Byte.BYTES;
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean[] read(final MemorySegment segment, final long position) {
    int size = segment.get(JAVA_INT_UNALIGNED, position);
    int length = (size - Integer.BYTES) / Byte.BYTES;
    boolean[] values = new boolean[length];
    for (int index = 0; index < length; index++) {
      values[index] = segment.get(JAVA_BYTE, position + Integer.BYTES + index * Byte.BYTES) == 1;
    }
    return values;
  }
}
