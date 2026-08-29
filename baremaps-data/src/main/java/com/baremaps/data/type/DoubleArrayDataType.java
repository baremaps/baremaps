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

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;

import java.lang.foreign.MemorySegment;

/**
 * A {@link DataType} for reading and writing arrays of double values in {@link MemorySegment}s.
 */
public class DoubleArrayDataType implements DataType<double[]> {

  /** {@inheritDoc} */
  @Override
  public int size(final double[] values) {
    return Integer.BYTES + values.length * Double.BYTES;
  }

  /** {@inheritDoc} */
  @Override
  public int size(final MemorySegment segment, final long position) {
    return segment.get(JAVA_INT_UNALIGNED, position);
  }

  /** {@inheritDoc} */
  @Override
  public void write(final MemorySegment segment, final long position, final double[] values) {
    segment.set(JAVA_INT_UNALIGNED, position, size(values));
    MemorySegment.copy(values, 0, segment, JAVA_DOUBLE_UNALIGNED, position + Integer.BYTES,
        values.length);
  }

  /** {@inheritDoc} */
  @Override
  public double[] read(final MemorySegment segment, final long position) {
    int size = segment.get(JAVA_INT_UNALIGNED, position);
    int length = (size - Integer.BYTES) / Double.BYTES;
    double[] values = new double[length];
    MemorySegment.copy(segment, JAVA_DOUBLE_UNALIGNED, position + Integer.BYTES, values, 0, length);
    return values;
  }
}
