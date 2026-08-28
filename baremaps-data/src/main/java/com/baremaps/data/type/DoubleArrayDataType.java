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
 * A {@link DataType} for reading and writing arrays of double values in {@link ByteBuffer}s.
 */
public class DoubleArrayDataType implements DataType<double[]> {

  /** {@inheritDoc} */
  @Override
  public int size(final double[] values) {
    return Integer.BYTES + values.length * Double.BYTES;
  }

  /** {@inheritDoc} */
  @Override
  public int size(final ByteBuffer buffer, final int position) {
    return buffer.getInt(position);
  }

  /** {@inheritDoc} */
  @Override
  public void write(final ByteBuffer buffer, final int position, final double[] values) {
    buffer.putInt(position, size(values));
    int p = position + Integer.BYTES;
    for (double value : values) {
      buffer.putDouble(p, value);
      p += Double.BYTES;
    }
  }

  /** {@inheritDoc} */
  @Override
  public double[] read(final ByteBuffer buffer, final int position) {
    int size = buffer.getInt(position);
    int length = (size - Integer.BYTES) / Double.BYTES;
    double[] values = new double[length];
    for (int index = 0; index < length; index++) {
      values[index] = buffer.getDouble(position + Integer.BYTES + index * Double.BYTES);
    }
    return values;
  }
}
