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

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;

import java.lang.foreign.MemorySegment;
import org.locationtech.jts.geom.Coordinate;

/**
 * A {@link DataType} for reading and writing arrays of {@link Coordinate} values in
 * {@link MemorySegment}s.
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
  public int size(final MemorySegment segment, final long position) {
    return segment.get(JAVA_INT_UNALIGNED, position);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final MemorySegment segment, final long position, final Coordinate[] value) {
    segment.set(JAVA_INT_UNALIGNED, position, size(value));
    long p = position + Integer.BYTES;
    for (Coordinate coordinate : value) {
      segment.set(JAVA_DOUBLE_UNALIGNED, p, coordinate.x);
      segment.set(JAVA_DOUBLE_UNALIGNED, p + Double.BYTES, coordinate.y);
      p += 2 * Double.BYTES;
    }
  }

  @Override
  public Coordinate[] read(final MemorySegment segment, final long position) {
    // Reading is one bulk copy instead of two checked accesses per coordinate; writing stays a
    // loop, as a temporary array costs more than the checks there.
    int size = segment.get(JAVA_INT_UNALIGNED, position);
    double[] ordinates = new double[(size - Integer.BYTES) / Double.BYTES];
    MemorySegment.copy(segment, JAVA_DOUBLE_UNALIGNED, position + Integer.BYTES, ordinates, 0,
        ordinates.length);
    Coordinate[] coordinates = new Coordinate[ordinates.length / 2];
    for (int i = 0; i < coordinates.length; i++) {
      coordinates[i] = new Coordinate(ordinates[2 * i], ordinates[2 * i + 1]);
    }
    return coordinates;
  }
}
