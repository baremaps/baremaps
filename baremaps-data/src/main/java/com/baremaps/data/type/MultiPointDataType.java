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
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;

/**
 * A {@link DataType} for reading and writing {@link MultiPoint} objects in {@link MemorySegment}s.
 */
public class MultiPointDataType implements DataType<MultiPoint> {

  private final CoordinateArrayDataType coordinateArrayDataType = new CoordinateArrayDataType();

  private final GeometryFactory geometryFactory;

  /**
   * Constructs a {@link MultiPointDataType} with a default {@link GeometryFactory}.
   */
  public MultiPointDataType() {
    this(new GeometryFactory());
  }

  /**
   * Constructs a {@link MultiPointDataType} with a specified {@link GeometryFactory}.
   *
   * @param geometryFactory the geometry factory
   */
  public MultiPointDataType(GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size(final MultiPoint value) {
    return coordinateArrayDataType.size(value.getCoordinates());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size(final MemorySegment segment, final long position) {
    return coordinateArrayDataType.size(segment, position);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final MemorySegment segment, final long position, final MultiPoint value) {
    coordinateArrayDataType.write(segment, position, value.getCoordinates());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MultiPoint read(final MemorySegment segment, final long position) {
    var coordinates = coordinateArrayDataType.read(segment, position);
    return geometryFactory.createMultiPoint(coordinates);
  }
}
