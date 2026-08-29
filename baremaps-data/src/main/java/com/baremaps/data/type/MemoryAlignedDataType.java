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
 * A {@link DataType} for reading and writing values in {@link ByteBuffer}s whose size is memory
 * aligned.
 *
 * @param <T> the type of value being read or written
 */
public abstract class MemoryAlignedDataType<T> extends FixedSizeDataType<T> {

  /**
   * Constructs a {@link MemoryAlignedDataType} with a memory-aligned size.
   *
   * @param size the size of the value in bytes (must be a power of 2)
   * @throws IllegalArgumentException if the size is not a power of 2
   */
  protected MemoryAlignedDataType(int size) {
    super(size);
    if ((size & -size) != size) {
      throw new IllegalArgumentException("The size must be a power of 2");
    }
  }
}
