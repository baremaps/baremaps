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

import java.lang.foreign.MemorySegment;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/** A {@link DataType} for {@link Point}s: two doubles, NaN for the empty point. */
public class PointDataType extends MemoryAlignedDataType<Point> {

  private final GeometryFactory geometryFactory;

  public PointDataType() {
    this(new GeometryFactory());
  }

  public PointDataType(GeometryFactory geometryFactory) {
    super(Double.BYTES * 2);
    this.geometryFactory = geometryFactory;
  }

  @Override
  public void write(final MemorySegment segment, final long position, final Point value) {
    if (value.isEmpty()) {
      segment.set(JAVA_DOUBLE_UNALIGNED, position, Double.NaN);
      segment.set(JAVA_DOUBLE_UNALIGNED, position + Double.BYTES, Double.NaN);
    } else {
      segment.set(JAVA_DOUBLE_UNALIGNED, position, value.getX());
      segment.set(JAVA_DOUBLE_UNALIGNED, position + Double.BYTES, value.getY());
    }
  }

  @Override
  public Point read(final MemorySegment segment, final long position) {
    double x = segment.get(JAVA_DOUBLE_UNALIGNED, position);
    double y = segment.get(JAVA_DOUBLE_UNALIGNED, position + Double.BYTES);
    if (Double.isNaN(x) || Double.isNaN(y)) {
      return geometryFactory.createPoint();
    }
    return geometryFactory.createPoint(new Coordinate(x, y));
  }
}
