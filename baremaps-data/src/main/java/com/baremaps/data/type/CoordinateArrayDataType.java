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

/**
 * A {@link DataType} for reading and writing arrays of {@link Coordinate} values in
 * {@link ByteBuffer}s.
 */
public class CoordinateArrayDataType implements DataType<Coordinate[]> {

  /**
   * {@inheritDoc}
   */
  @Override
  public int size(final Coordinate[] value) {
    return Integer.BYTES + Double.BYTES * 2 * value.length;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size(final ByteBuffer buffer, final int position) {
    return buffer.getInt(position);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final ByteBuffer buffer, final int position, final Coordinate[] value) {
    buffer.putInt(position, size(value));
    int p = position + Integer.BYTES;
    for (int i = 0; i < value.length; i++) {
      Coordinate coordinate = value[i];
      buffer.putDouble(p, coordinate.x);
      p += Double.BYTES;
      buffer.putDouble(p, coordinate.y);
      p += Double.BYTES;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Coordinate[] read(final ByteBuffer buffer, final int position) {
    int size = buffer.getInt(position);
    int numPoints = (size - Integer.BYTES) / (Double.BYTES * 2);
    int p = position + Integer.BYTES;
    Coordinate[] coordinates = new Coordinate[numPoints];
    for (int i = 0; i < numPoints; i++) {
      double x = buffer.getDouble(p);
      double y = buffer.getDouble(p + Double.BYTES);
      coordinates[i] = new Coordinate(x, y);
      p += Double.BYTES * 2;
    }
    return coordinates;
  }
}
