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
import org.locationtech.jts.geom.Coordinate;

/** A {@link DataType} for reading and writing {@link Coordinate} values in {@link ByteBuffer}s. */
public class CoordinateDataType extends MemoryAlignedDataType<Coordinate> {

  /**
   * Constructs a {@link CoordinateDataType} with a fixed size of two {@link Double#BYTES}.
   */
  public CoordinateDataType() {
    super(Double.BYTES * 2);
  }

  /** {@inheritDoc} */
  @Override
  public void write(final ByteBuffer buffer, final int position, final Coordinate value) {
    buffer.putDouble(position, value.x);
    buffer.putDouble(position + Double.BYTES, value.y);
  }

  /** {@inheritDoc} */
  @Override
  public Coordinate read(final ByteBuffer buffer, final int position) {
    double x = buffer.getDouble(position);
    double y = buffer.getDouble(position + Double.BYTES);
    return new Coordinate(x, y);
  }
}
