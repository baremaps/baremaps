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

import java.lang.foreign.MemorySegment;

/**
 * A {@link DataType} for reading and writing small integer values in {@link MemorySegment}s with a
 * customizable storage size.
 */
public class SmallIntegerDataType extends FixedSizeDataType<Integer> {

  private final int n;

  /**
   * Constructs a {@link SmallIntegerDataType} with a specified number of bytes.
   *
   * @param n the number of bytes used to store the integer (must be between 1 and 4)
   * @throws IllegalArgumentException if n is less than 1 or greater than 4
   */
  public SmallIntegerDataType(int n) {
    super(checkSize(n));
    this.n = n;
  }

  private static int checkSize(int n) {
    if (n < 1 || n > 4) {
      throw new IllegalArgumentException("The number of bytes must be between 1 and 4");
    }
    return n;
  }

  /** {@inheritDoc} */
  @Override
  public void write(final MemorySegment segment, final long position, final Integer value) {
    for (int i = 0; i < n; i++) {
      segment.set(JAVA_BYTE, position + i, (byte) (value >> (i << 3)));
    }
  }

  /** {@inheritDoc} */
  @Override
  public Integer read(final MemorySegment segment, final long position) {
    byte s = (byte) (segment.get(JAVA_BYTE, position + n - 1) >= 0 ? 0 : -1);
    int l = 0;
    for (int i = 3; i > n - 1; i--) {
      l |= (s & 0xff) << (i << 3);
    }
    for (int i = n - 1; i >= 0; i--) {
      l |= (segment.get(JAVA_BYTE, position + i) & 0xff) << (i << 3);
    }
    return l;
  }
}
