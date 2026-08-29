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

import static java.lang.Boolean.TRUE;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;

import java.lang.foreign.MemorySegment;

/**
 * A {@link DataType} for reading and writing boolean values in {@link MemorySegment}s.
 */
public class BooleanDataType extends MemoryAlignedDataType<Boolean> {

  /**
   * Constructs a {@link BooleanDataType} with a fixed size of {@link Byte#BYTES}.
   */
  public BooleanDataType() {
    super(Byte.BYTES);
  }

  /** {@inheritDoc} */
  @Override
  public void write(final MemorySegment segment, final long position, final Boolean value) {
    segment.set(JAVA_BYTE, position, TRUE.equals(value) ? (byte) 1 : (byte) 0);
  }

  /** {@inheritDoc} */
  @Override
  public Boolean read(final MemorySegment segment, final long position) {
    return segment.get(JAVA_BYTE, position) == 1;
  }
}
