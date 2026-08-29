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
import org.locationtech.jts.geom.LineString;

/**
 * A {@link DataType} for reading and writing {@link LineString} objects in {@link MemorySegment}s.
 */
public class LineStringDataType implements DataType<LineString> {

  private final GeometryFactory geometryFactory;

  private final CoordinateArrayDataType coordinateArrayDataType;

  /**
   * Constructs a {@link LineStringDataType} with a default {@link GeometryFactory}.
   */
  public LineStringDataType() {
    this(new GeometryFactory());
  }

  /**
   * Constructs a {@link LineStringDataType} with a specified {@link GeometryFactory}.
   *
   * @param geometryFactory the geometry factory
   */
  public LineStringDataType(GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
    this.coordinateArrayDataType = new CoordinateArrayDataType();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size(final LineString value) {
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
  public void write(final MemorySegment segment, final long position, final LineString value) {
    coordinateArrayDataType.write(segment, position, value.getCoordinates());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LineString read(final MemorySegment segment, final long position) {
    var coordinates = coordinateArrayDataType.read(segment, position);
    return geometryFactory.createLineString(coordinates);
  }
}
