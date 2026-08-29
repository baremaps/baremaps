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

import com.baremaps.shapefile.Shapefile.GeometryType;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.locationtech.jts.algorithm.Area;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.PointLocation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

/**
 * Decodes the geometries of a {@code .shp} file, in the order the file stores them.
 * <p>
 * The records are read one after the other rather than sought through the {@code .shx} index: that
 * index is optional in the specification, and a file that lacks one is not otherwise different. The
 * length a record declares for itself is what moves the cursor to the next one, so a geometry this
 * class decodes only in part, or does not decode at all, still leaves the records that follow it
 * readable.
 */
class GeometryDecoder {

  /** The header of the file, whose fixed size is what the first record is offset by. */
  private static final int HEADER_SIZE = 100;

  /** The value the specification gives to the first field of the header. */
  private static final int FILE_CODE = 9994;

  /** A record number and a content length, four bytes each, precede the content of a record. */
  private static final int RECORD_HEADER_SIZE = 2 * Integer.BYTES;

  private final ByteBuffer buffer;

  private final GeometryFactory geometryFactory;

  private final Shapefile.Header header;

  /** The offset the last record ends at, past which the file holds nothing to read. */
  private final int end;

  GeometryDecoder(ByteBuffer buffer, GeometryFactory geometryFactory) throws ShapefileException {
    this.buffer = buffer;
    this.geometryFactory = geometryFactory;

    buffer.order(ByteOrder.BIG_ENDIAN);
    if (buffer.capacity() < HEADER_SIZE || buffer.getInt(0) != FILE_CODE) {
      throw new ShapefileException("Not a shapefile: the file code of the header is not 9994");
    }
    // The header states the length of the whole file in 16 bit words. A writer that interrupted
    // itself leaves behind a length longer than the file, so the file itself has the final say.
    int declared = buffer.getInt(24) * 2;
    this.end = declared > HEADER_SIZE ? Math.min(declared, buffer.capacity()) : buffer.capacity();

    buffer.order(ByteOrder.LITTLE_ENDIAN);
    this.header = new Shapefile.Header(
        GeometryType.of(buffer.getInt(32)),
        new Envelope(buffer.getDouble(36), buffer.getDouble(52), buffer.getDouble(44),
            buffer.getDouble(60)),
        buffer.getDouble(68), buffer.getDouble(76),
        buffer.getDouble(84), buffer.getDouble(92));

    buffer.position(HEADER_SIZE);
  }

  Shapefile.Header header() {
    return header;
  }

  /**
   * Decodes the next geometry, or returns null once the file holds no further record. A record of
   * the null shape type also decodes to null: the specification uses it for a feature whose
   * attributes are known and whose geometry is not.
   */
  Geometry next() throws ShapefileException {
    if (buffer.position() + RECORD_HEADER_SIZE > end) {
      return null;
    }

    buffer.order(ByteOrder.BIG_ENDIAN);
    int number = buffer.getInt();
    int length = buffer.getInt() * 2;
    int content = buffer.position();
    if (length < Integer.BYTES || content + length > end) {
      throw new ShapefileException(
          "Record %d declares %d bytes of content, which the file does not hold"
              .formatted(number, length));
    }

    // Reading the record against a limit of its own turns every overrun of a malformed record into
    // a buffer underflow, which spares each decoder below a bound check of its own.
    int limit = buffer.limit();
    try {
      buffer.limit(content + length);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      return decode(GeometryType.of(buffer.getInt()));
    } catch (BufferUnderflowException | IllegalArgumentException e) {
      throw new ShapefileException("Record %d is malformed".formatted(number), e);
    } finally {
      buffer.limit(limit);
      buffer.position(content + length);
    }
  }

  private Geometry decode(GeometryType type) throws ShapefileException {
    return switch (type) {
      case NULL -> null;
      case POINT, POINT_Z, POINT_M -> point();
      case MULTIPOINT, MULTIPOINT_Z, MULTIPOINT_M -> multiPoint();
      case POLYLINE, POLYLINE_Z, POLYLINE_M -> polyline();
      case POLYGON, POLYGON_Z, POLYGON_M -> polygon();
      // A multipatch is a mesh of triangle strips and fans, which has no counterpart among the
      // geometries this reader produces.
      case MULTIPATCH -> throw new ShapefileException("Multipatch geometries are not supported");
    };
  }

  private Geometry point() {
    double x = buffer.getDouble();
    double y = buffer.getDouble();
    return geometryFactory.createPoint(new Coordinate(x, y));
  }

  private Geometry multiPoint() {
    skipEnvelope();
    return geometryFactory.createMultiPointFromCoords(coordinates(buffer.getInt()));
  }

  private Geometry polyline() {
    Coordinate[][] parts = parts();
    LineString[] lines = new LineString[parts.length];
    for (int i = 0; i < parts.length; i++) {
      lines[i] = geometryFactory.createLineString(parts[i]);
    }
    // The parts of a polyline are disjoint, so joining them would invent segments the file does
    // not contain.
    return lines.length == 1 ? lines[0] : geometryFactory.createMultiLineString(lines);
  }

  private Geometry polygon() {
    // The specification orders the vertices of an outer ring clockwise and those of a hole
    // counter-clockwise, but it does not record which outer ring a hole belongs to.
    List<LinearRing> shells = new ArrayList<>();
    List<LinearRing> holes = new ArrayList<>();
    for (Coordinate[] part : parts()) {
      LinearRing ring = geometryFactory.createLinearRing(part);
      (Orientation.isCCW(part) ? holes : shells).add(ring);
    }
    if (shells.isEmpty()) {
      // Writers that disregard the ordering rule emit every ring counter-clockwise. Reading those
      // rings as holes of nothing would drop the geometry, so read them as outer rings instead.
      shells = holes;
      holes = List.of();
    }

    List<List<LinearRing>> assigned = new ArrayList<>(shells.size());
    for (int i = 0; i < shells.size(); i++) {
      assigned.add(new ArrayList<>());
    }
    for (LinearRing hole : holes) {
      int owner = enclosingShell(shells, hole);
      if (owner < 0) {
        // A hole no ring encloses cannot be subtracted from anything. Reading it as an outer ring
        // keeps the area the file describes instead of discarding it.
        shells.add(hole);
        assigned.add(new ArrayList<>());
      } else {
        assigned.get(owner).add(hole);
      }
    }

    Polygon[] polygons = new Polygon[shells.size()];
    for (int i = 0; i < polygons.length; i++) {
      polygons[i] = geometryFactory.createPolygon(shells.get(i),
          assigned.get(i).toArray(LinearRing[]::new));
    }
    return polygons.length == 1 ? polygons[0] : geometryFactory.createMultiPolygon(polygons);
  }

  /**
   * The index of the smallest ring of {@code shells} that encloses {@code hole}, or -1 if none
   * does. The smallest one owns the hole because a ring may itself sit inside a hole of a larger
   * ring, and the hole then belongs to the inner ring rather than to the outer one.
   */
  private static int enclosingShell(List<LinearRing> shells, LinearRing hole) {
    Coordinate vertex = hole.getCoordinateN(0);
    int owner = -1;
    double smallest = Double.POSITIVE_INFINITY;
    for (int i = 0; i < shells.size(); i++) {
      Coordinate[] shell = shells.get(i).getCoordinates();
      double area = Area.ofRing(shell);
      if (area < smallest && PointLocation.isInRing(vertex, shell)) {
        smallest = area;
        owner = i;
      }
    }
    return owner;
  }

  /** Reads the parts of a polyline or of a polygon, as one array of coordinates each. */
  private Coordinate[][] parts() {
    skipEnvelope();
    int numParts = buffer.getInt();
    int numPoints = buffer.getInt();
    requireContent(numParts >= 0 && numPoints >= 0);

    // The offset of the first vertex of every part, followed by the end of the last part, so that
    // each part is the range between two consecutive entries.
    int[] offsets = new int[numParts + 1];
    for (int i = 0; i < numParts; i++) {
      offsets[i] = buffer.getInt();
    }
    offsets[numParts] = numPoints;

    Coordinate[] coordinates = coordinates(numPoints);
    Coordinate[][] parts = new Coordinate[numParts][];
    for (int i = 0; i < numParts; i++) {
      requireContent(offsets[i] >= 0 && offsets[i] <= offsets[i + 1]);
      parts[i] = Arrays.copyOfRange(coordinates, offsets[i], offsets[i + 1]);
    }
    return parts;
  }

  private Coordinate[] coordinates(int count) {
    // Checked before allocating, so that a corrupt count cannot ask for an array the record could
    // never fill.
    requireContent(count >= 0 && (long) count * 2 * Double.BYTES <= buffer.remaining());
    Coordinate[] coordinates = new Coordinate[count];
    for (int i = 0; i < count; i++) {
      double x = buffer.getDouble();
      double y = buffer.getDouble();
      coordinates[i] = new Coordinate(x, y);
    }
    return coordinates;
  }

  /** Every geometry but a point repeats its own bounds, which its coordinates already give. */
  private void skipEnvelope() {
    buffer.position(buffer.position() + 4 * Double.BYTES);
  }

  /**
   * Rejects a count or an offset that the record cannot hold. It raises the
   * {@link BufferUnderflowException} the buffer itself raises when a record runs out, because it
   * reports the same thing: the record describes more content than it carries.
   */
  private static void requireContent(boolean valid) {
    if (!valid) {
      throw new BufferUnderflowException();
    }
  }
}
