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

import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

/**
 * A {@link DataType} for reading and writing {@link Polygon} objects in {@link MemorySegment}s.
 */
public class PolygonDataType implements DataType<Polygon> {

  private final CoordinateArrayDataType coordinateArrayDataType = new CoordinateArrayDataType();

  private final GeometryFactory geometryFactory;

  /**
   * Constructs a {@link PolygonDataType} with a default {@link GeometryFactory}.
   */
  public PolygonDataType() {
    this(new GeometryFactory());
  }

  /**
   * Constructs a {@link PolygonDataType} with a specified {@link GeometryFactory}.
   *
   * @param geometryFactory the geometry factory
   */
  public PolygonDataType(GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public int size(final Polygon value) {
    int size = Integer.BYTES;

    // Add the size of the exterior ring
    var exteriorRing = value.getExteriorRing();
    size += coordinateArrayDataType.size(exteriorRing.getCoordinates());

    // Add the size of the interior rings
    for (int i = 0; i < value.getNumInteriorRing(); i++) {
      var interiorRing = value.getInteriorRingN(i);
      size += coordinateArrayDataType.size(interiorRing.getCoordinates());
    }

    return size;
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
  public void write(final MemorySegment segment, final long position, final Polygon value) {
    segment.set(JAVA_INT_UNALIGNED, position, size(value));
    long p = position + Integer.BYTES;

    // Write the exterior ring
    var exteriorRing = value.getExteriorRing();
    coordinateArrayDataType.write(segment, p, exteriorRing.getCoordinates());
    p += coordinateArrayDataType.size(exteriorRing.getCoordinates());

    // Write the interior rings
    for (int i = 0; i < value.getNumInteriorRing(); i++) {
      var interiorRing = value.getInteriorRingN(i);
      coordinateArrayDataType.write(segment, p, interiorRing.getCoordinates());
      p += coordinateArrayDataType.size(interiorRing.getCoordinates());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Polygon read(final MemorySegment segment, final long position) {
    var size = size(segment, position);
    long limit = position + size;
    long p = position + Integer.BYTES;


    // Read the exterior ring
    var exteriorRingCoordinates = coordinateArrayDataType.read(segment, p);
    var exteriorRing = geometryFactory.createLinearRing(exteriorRingCoordinates);
    p += coordinateArrayDataType.size(segment, p);

    // Read the interior rings
    var interiorRings = new ArrayList<LineString>();
    while (p < limit) {
      var interiorRingCoordinates = coordinateArrayDataType.read(segment, p);
      var interiorRing = geometryFactory.createLinearRing(interiorRingCoordinates);
      interiorRings.add(interiorRing);
      p += coordinateArrayDataType.size(segment, p);
    }

    return geometryFactory.createPolygon(exteriorRing, interiorRings.toArray(LinearRing[]::new));
  }
}
