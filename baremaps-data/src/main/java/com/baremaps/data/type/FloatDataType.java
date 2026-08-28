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
 * A {@link DataType} for reading and writing float values in {@link ByteBuffer}s.
 */
public class FloatDataType extends MemoryAlignedDataType<Float> {

  /**
   * Constructs a {@link FloatDataType} with a fixed size of {@link Float#BYTES}.
   */
  public FloatDataType() {
    super(Float.BYTES);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final ByteBuffer buffer, final int position, final Float value) {
    buffer.putFloat(position, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Float read(final ByteBuffer buffer, final int position) {
    return buffer.getFloat(position);
  }
}
