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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.MemorySegment;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.PrecisionModel;

/** Edge cases of the geometry types: empties, rings, holes, nesting, factories, nulls. */
class GeometryDataTypeTest {

  private final GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);

  private final GeometryDataType type = new GeometryDataType(factory);

  private Geometry roundTrip(Geometry geometry) {
    var buffer = MemorySegment.ofArray(new byte[type.size(geometry) + 8]);
    type.write(buffer, 4, geometry);
    assertEquals(type.size(geometry), type.size(buffer, 4));
    return type.read(buffer, 4);
  }

  private Coordinate[] square() {
    return new Coordinate[] {
        new Coordinate(0, 0), new Coordinate(0, 10), new Coordinate(10, 10),
        new Coordinate(10, 0), new Coordinate(0, 0)};
  }

  @Test
  void empties() {
    for (Geometry empty : new Geometry[] {
        factory.createPoint(),
        factory.createLineString(),
        factory.createPolygon(),
        factory.createMultiPoint(),
        factory.createMultiLineString(),
        factory.createMultiPolygon(),
        factory.createGeometryCollection()}) {
      Geometry read = roundTrip(empty);
      assertTrue(read.isEmpty(), empty.getGeometryType());
      assertEquals(empty.getGeometryType(), read.getGeometryType());
    }
  }

  @Test
  void polygonWithHoles() {
    LinearRing shell = factory.createLinearRing(square());
    LinearRing hole = factory.createLinearRing(new Coordinate[] {
        new Coordinate(2, 2), new Coordinate(2, 4), new Coordinate(4, 4),
        new Coordinate(4, 2), new Coordinate(2, 2)});
    var polygon = factory.createPolygon(shell, new LinearRing[] {hole});
    var read = roundTrip(polygon);
    assertTrue(polygon.equalsExact(read));
    assertEquals(1, ((org.locationtech.jts.geom.Polygon) read).getNumInteriorRing());
  }

  @Test
  void linearRingReadsBackAsLineString() {
    var ring = factory.createLinearRing(square());
    var read = roundTrip(ring);
    assertEquals("LineString", read.getGeometryType());
    assertTrue(ring.equalsTopo(read));
  }

  @Test
  void nestedCollections() {
    var inner = factory.createGeometryCollection(new Geometry[] {
        factory.createPoint(new Coordinate(1, 1)),
        factory.createMultiPoint(new org.locationtech.jts.geom.Point[] {
            factory.createPoint(new Coordinate(2, 2))})});
    var outer = factory.createGeometryCollection(new Geometry[] {
        inner, factory.createPolygon(square())});
    assertTrue(outer.equalsExact(roundTrip(outer)));
  }

  @Test
  void factoryIsPropagated() {
    var collection = factory.createGeometryCollection(new Geometry[] {
        factory.createPoint(new Coordinate(1, 1)),
        factory.createMultiPolygon(new org.locationtech.jts.geom.Polygon[] {
            factory.createPolygon(square())})});
    var read = roundTrip(collection);
    assertEquals(4326, read.getSRID());
    for (int i = 0; i < read.getNumGeometries(); i++) {
      assertEquals(4326, read.getGeometryN(i).getSRID());
      assertEquals(factory, read.getGeometryN(i).getFactory());
    }
  }

  @Test
  void nullsAreRejected() {
    assertThrows(NullPointerException.class, () -> type.size(null));
    assertThrows(NullPointerException.class, () -> new WKBDataType().size(null));
  }

  @Test
  void zeroTagIsNotAGeometry() {
    var zeros = MemorySegment.ofArray(new byte[64]);
    assertThrows(IllegalArgumentException.class, () -> type.read(zeros, 0));
  }
}
