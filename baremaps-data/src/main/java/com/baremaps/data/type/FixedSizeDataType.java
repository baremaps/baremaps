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
 * A {@link DataType} for reading and writing values in {@link MemorySegment}s whose size is fixed.
 *
 * @param <T> the type of value being read or written
 */
public abstract class FixedSizeDataType<T> implements DataType<T> {

  private final int size;

  /**
   * Creates a {@link FixedSizeDataType} with a fixed size.
   *
   * @param size the size of the value in bytes
   */
  protected FixedSizeDataType(final int size) {
    this.size = size;
  }

  /** {@inheritDoc} */
  @Override
  public int size(final T value) {
    return size;
  }

  /** {@inheritDoc} */
  @Override
  public int size(final MemorySegment segment, final long position) {
    return size;
  }

  /**
   * Returns the fixed size of this data type.
   *
   * @return the size in bytes
   */
  public int size() {
    return size;
  }

}
