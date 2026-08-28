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
 * A {@link DataType} for reading and writing small long values in {@link ByteBuffer}s with a
 * customizable storage size.
 */
public class SmallLongDataType extends FixedSizeDataType<Long> {

  private final int n;

  /**
   * Constructs a {@link SmallLongDataType} with a specified number of bytes.
   *
   * @param n the number of bytes used to store the long value (must be between 1 and 8)
   * @throws IllegalArgumentException if n is less than 1 or greater than 8
   */
  public SmallLongDataType(int n) {
    super(n);
    if (n < 1 || n > 8) {
      throw new IllegalArgumentException(
          "The number of bytes used to store small longs must be comprised between 1 and 8");
    }
    this.n = n;
  }

  /** {@inheritDoc} */
  @Override
  public int size(final Long value) {
    return n;
  }

  /** {@inheritDoc} */
  @Override
  public void write(final ByteBuffer buffer, final int position, final Long value) {
    for (int i = 0; i < n; i++) {
      buffer.put(position + i, (byte) (value >> (i << 3)));
    }
  }

  /** {@inheritDoc} */
  @Override
  public Long read(final ByteBuffer buffer, final int position) {
    byte s = (byte) (buffer.get(position + n - 1) >= 0 ? 0 : -1);
    long l = 0;
    for (int i = 7; i > n - 1; i--) {
      l |= ((long) s & 0xff) << (i << 3);
    }
    for (int i = n - 1; i >= 0; i--) {
      l |= ((long) buffer.get(position + i) & 0xff) << (i << 3);
    }
    return l;
  }
}
