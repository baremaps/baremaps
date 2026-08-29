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

package com.baremaps.shapefile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.shapefile.Shapefile.GeometryType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

class GeometryDecoderTest {

  private static final GeometryFactory FACTORY = new GeometryFactory();

  @Test
  void readsTheHeader() throws Exception {
    GeometryDecoder decoder = decoder(GeometryType.POINT);
    assertEquals(GeometryType.POINT, decoder.header().geometryType());
    // The header interleaves the bounds as xmin, ymin, xmax, ymax.
    assertEquals(new Envelope(1, 3, 2, 4), decoder.header().envelope());
  }

  @Test
  void readsPoints() throws Exception {
    GeometryDecoder decoder = decoder(GeometryType.POINT, point(1, 2), point(3, 4));
    assertEquals(FACTORY.createPoint(new Coordinate(1, 2)),
        decoder.next());
    assertEquals(FACTORY.createPoint(new Coordinate(3, 4)),
        decoder.next());
    assertNull(decoder.next());
  }

  @Test
  void readsANullShapeAsNoGeometry() throws Exception {
    GeometryDecoder decoder = decoder(GeometryType.POINT, nullShape(), point(1, 2));
    assertNull(decoder.next());
    assertInstanceOf(Point.class, decoder.next());
  }

  @Test
  void readsMultiPoints() throws Exception {
    GeometryDecoder decoder =
        decoder(GeometryType.MULTIPOINT, multiPoint(new double[] {1, 2}, new double[] {3, 4}));
    MultiPoint geometry = assertInstanceOf(MultiPoint.class, decoder.next());
    assertEquals(2, geometry.getNumGeometries());
  }

  @Test
  void readsASinglePartPolylineAsALineString() throws Exception {
    GeometryDecoder decoder = decoder(GeometryType.POLYLINE,
        shape(3, new double[][] {{0, 0}, {1, 1}, {2, 0}}));
    LineString geometry = assertInstanceOf(LineString.class, decoder.next());
    assertEquals(3, geometry.getNumPoints());
  }

  @Test
  void keepsThePartsOfAMultipartPolylineApart() throws Exception {
    GeometryDecoder decoder = decoder(GeometryType.POLYLINE,
        shape(3, new double[][] {{0, 0}, {1, 1}}, new double[][] {{10, 10}, {11, 11}}));
    MultiLineString geometry = assertInstanceOf(MultiLineString.class, decoder.next());
    assertEquals(2, geometry.getNumGeometries());
    assertEquals(2, geometry.getGeometryN(0).getNumPoints());
    assertEquals(2, geometry.getGeometryN(1).getNumPoints());
  }

  @Test
  void readsAPolygonWithoutHoles() throws Exception {
    GeometryDecoder decoder = decoder(GeometryType.POLYGON, shape(5, clockwise(0, 0, 10, 10)));
    Polygon geometry = assertInstanceOf(Polygon.class, decoder.next());
    assertEquals(0, geometry.getNumInteriorRing());
    assertEquals(100, geometry.getArea());
  }

  @Test
  void readsACounterClockwiseRingAsAHole() throws Exception {
    GeometryDecoder decoder = decoder(GeometryType.POLYGON,
        shape(5, clockwise(0, 0, 10, 10), counterClockwise(2, 2, 8, 8)));
    Polygon geometry = assertInstanceOf(Polygon.class, decoder.next());
    assertEquals(1, geometry.getNumInteriorRing());
    assertEquals(100 - 36, geometry.getArea());
  }

  @Test
  void givesEachHoleToTheSmallestRingThatEnclosesIt() throws Exception {
    // A ring inside the hole of a larger ring owns the holes it encloses itself.
    GeometryDecoder decoder = decoder(GeometryType.POLYGON, shape(5,
        clockwise(0, 0, 100, 100),
        counterClockwise(10, 10, 90, 90),
        clockwise(20, 20, 80, 80),
        counterClockwise(30, 30, 70, 70)));

    MultiPolygon geometry = assertInstanceOf(MultiPolygon.class, decoder.next());
    assertEquals(2, geometry.getNumGeometries());
    Polygon outer = (Polygon) geometry.getGeometryN(0);
    Polygon inner = (Polygon) geometry.getGeometryN(1);
    assertEquals(100 * 100 - 80 * 80, outer.getArea());
    assertEquals(60 * 60 - 40 * 40, inner.getArea());
  }

  @Test
  void readsRingsAsOuterRingsWhenTheWriterOrdersThemAllCounterClockwise() throws Exception {
    GeometryDecoder decoder = decoder(GeometryType.POLYGON,
        shape(5, counterClockwise(0, 0, 10, 10), counterClockwise(20, 0, 30, 10)));
    MultiPolygon geometry = assertInstanceOf(MultiPolygon.class, decoder.next());
    assertEquals(2, geometry.getNumGeometries());
    assertEquals(200, geometry.getArea());
  }

  @Test
  void movesToTheNextRecordByTheLengthTheRecordDeclares() throws Exception {
    // A record longer than the geometry it holds, which the reader must skip past whole.
    byte[] padded = new byte[point(1, 2).length + 16];
    System.arraycopy(point(1, 2), 0, padded, 0, point(1, 2).length);

    GeometryDecoder decoder = decoder(GeometryType.POINT, padded, point(3, 4));
    assertInstanceOf(Point.class, decoder.next());
    Point second = assertInstanceOf(Point.class, decoder.next());
    assertEquals(3, second.getX());
  }

  @Test
  void rejectsAFileThatIsNotAShapefile() {
    ByteBuffer buffer = shapefile(GeometryType.POINT.value(), point(1, 2));
    buffer.order(ByteOrder.BIG_ENDIAN).putInt(0, 1234);
    assertThrows(ShapefileException.class, () -> new GeometryDecoder(buffer, FACTORY));
  }

  @Test
  void rejectsARecordLongerThanTheFile() throws Exception {
    ByteBuffer buffer = shapefile(GeometryType.POINT.value(), point(1, 2));
    // The content length of the first record, in 16 bit words, at the offset the header ends at.
    buffer.order(ByteOrder.BIG_ENDIAN).putInt(104, 1_000_000);
    GeometryDecoder decoder = new GeometryDecoder(buffer, FACTORY);
    assertThrows(ShapefileException.class, decoder::next);
  }

  @Test
  void rejectsARecordThatDeclaresMoreVerticesThanItHolds() throws Exception {
    byte[] shape = shape(3, new double[][] {{0, 0}, {1, 1}});
    ByteBuffer.wrap(shape).order(ByteOrder.LITTLE_ENDIAN).putInt(40, 1_000_000);
    GeometryDecoder decoder = decoder(GeometryType.POLYLINE, shape);
    assertThrows(ShapefileException.class, decoder::next);
  }

  @Test
  void rejectsAnUnknownShapeType() throws Exception {
    ByteBuffer content = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(99);
    GeometryDecoder decoder = decoder(GeometryType.POINT, content.array());
    assertThrows(ShapefileException.class, decoder::next);
  }

  @Test
  void rejectsMultipatchGeometries() throws Exception {
    ByteBuffer content = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        .putInt(GeometryType.MULTIPATCH.value());
    GeometryDecoder decoder = decoder(GeometryType.MULTIPATCH, content.array());
    assertThrows(ShapefileException.class, decoder::next);
  }

  private static GeometryDecoder decoder(GeometryType type, byte[]... records)
      throws ShapefileException {
    return new GeometryDecoder(shapefile(type.value(), records), FACTORY);
  }

  /** The bytes of a {@code .shp} file holding the given record contents. */
  private static ByteBuffer shapefile(int type, byte[]... records) {
    int content = 0;
    for (byte[] record : records) {
      content += 2 * Integer.BYTES + record.length;
    }

    ByteBuffer buffer = ByteBuffer.allocate(100 + content);
    buffer.order(ByteOrder.BIG_ENDIAN);
    buffer.putInt(0, 9994);
    buffer.putInt(24, (100 + content) / 2);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(28, 1000);
    buffer.putInt(32, type);
    buffer.putDouble(36, 1).putDouble(44, 2).putDouble(52, 3).putDouble(60, 4);

    buffer.position(100);
    for (int number = 1; number <= records.length; number++) {
      byte[] record = records[number - 1];
      buffer.order(ByteOrder.BIG_ENDIAN);
      buffer.putInt(number);
      buffer.putInt(record.length / 2);
      buffer.put(record);
    }
    return buffer;
  }

  private static byte[] nullShape() {
    return ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array();
  }

  private static byte[] point(double x, double y) {
    return ByteBuffer.allocate(Integer.BYTES + 2 * Double.BYTES).order(ByteOrder.LITTLE_ENDIAN)
        .putInt(1).putDouble(x).putDouble(y).array();
  }

  private static byte[] multiPoint(double[]... points) {
    ByteBuffer buffer = ByteBuffer
        .allocate(2 * Integer.BYTES + 4 * Double.BYTES + points.length * 2 * Double.BYTES)
        .order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(8);
    buffer.putDouble(0).putDouble(0).putDouble(0).putDouble(0);
    buffer.putInt(points.length);
    for (double[] point : points) {
      buffer.putDouble(point[0]).putDouble(point[1]);
    }
    return buffer.array();
  }

  /** The content of a polyline or polygon record, one array of vertices per part. */
  private static byte[] shape(int type, double[][]... parts) {
    int vertices = 0;
    for (double[][] part : parts) {
      vertices += part.length;
    }

    ByteBuffer buffer = ByteBuffer.allocate(3 * Integer.BYTES + 4 * Double.BYTES
        + parts.length * Integer.BYTES + vertices * 2 * Double.BYTES)
        .order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(type);
    buffer.putDouble(0).putDouble(0).putDouble(0).putDouble(0);
    buffer.putInt(parts.length);
    buffer.putInt(vertices);

    int offset = 0;
    for (double[][] part : parts) {
      buffer.putInt(offset);
      offset += part.length;
    }
    for (double[][] part : parts) {
      for (double[] vertex : part) {
        buffer.putDouble(vertex[0]).putDouble(vertex[1]);
      }
    }
    return buffer.array();
  }

  /** A closed rectangular ring, wound the way the specification winds an outer ring. */
  private static double[][] clockwise(double minX, double minY, double maxX, double maxY) {
    return new double[][] {{minX, minY}, {minX, maxY}, {maxX, maxY}, {maxX, minY}, {minX, minY}};
  }

  /** A closed rectangular ring, wound the way the specification winds a hole. */
  private static double[][] counterClockwise(double minX, double minY, double maxX, double maxY) {
    return new double[][] {{minX, minY}, {maxX, minY}, {maxX, maxY}, {minX, maxY}, {minX, minY}};
  }

  @Test
  void buildsRingsTheSpecificationWouldRecognize() {
    // Guards the fixtures themselves: the tests above mean nothing if the windings are swapped.
    Geometry clockwise = FACTORY.createPolygon(coordinates(clockwise(0, 0, 1, 1)));
    Geometry counterClockwise = FACTORY.createPolygon(coordinates(counterClockwise(0, 0, 1, 1)));
    assertFalse(Orientation.isCCW(clockwise.getCoordinates()));
    assertTrue(Orientation.isCCW(counterClockwise.getCoordinates()));
  }

  private static Coordinate[] coordinates(double[][] ring) {
    Coordinate[] coordinates =
        new Coordinate[ring.length];
    for (int i = 0; i < ring.length; i++) {
      coordinates[i] = new Coordinate(ring[i][0], ring[i][1]);
    }
    return coordinates;
  }
}
