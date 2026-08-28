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
 * A {@link DataType} for reading and writing byte values in {@link ByteBuffer}s.
 */
public class ByteDataType extends MemoryAlignedDataType<Byte> {

  /**
   * Constructs a {@link ByteDataType} with a fixed size of {@link Byte#BYTES}.
   */
  public ByteDataType() {
    super(Byte.BYTES);
  }

  /** {@inheritDoc} */
  @Override
  public void write(final ByteBuffer buffer, final int position, final Byte value) {
    buffer.put(position, value);
  }

  /** {@inheritDoc} */
  @Override
  public Byte read(final ByteBuffer buffer, final int position) {
    return buffer.get(position);
  }
}
