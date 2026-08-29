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

package com.baremaps.flatgeobuf;

import com.baremaps.flatgeobuf.FlatGeoBuf.GeometryType;
import com.baremaps.flatgeobuf.generated.Geometry;
import com.google.flatbuffers.FlatBufferBuilder;
import java.util.Arrays;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * The translation between JTS geometries and the geometry table of the FlatGeoBuf schema.
 * <p>
 * A FlatGeoBuf geometry is a flat run of ordinates plus, for the types made of several rings or
 * lines, the index at which each of them ends. Only the types the format calls simple are handled
 * here; curves and geometry collections are rejected rather than silently mangled.
 * <p>
 * This code has been adapted from FlatGeoBuf (BSD 2-Clause "Simplified" License).
 * <p>
 * Copyright (c) 2018, Bj&ouml;rn Harrtell
 */
class GeometryConversions {

  private GeometryConversions() {
    // Prevent instantiation
  }

  /**
   * Writes {@code geometry} and returns its offset in {@code builder}.
   *
   * @param headerType the type declared by the header, which is {@link GeometryType#UNKNOWN} for a
   *        file that mixes types
   * @param hasZ whether the file stores a Z ordinate, and likewise for {@code hasM}. The header
   *        decides, not the geometry: JTS reports a Z dimension for coordinates that merely have
   *        room for one, so trusting the geometry writes a vector of NaN for every 2D file.
   */
  static int write(
      FlatBufferBuilder builder,
      org.locationtech.jts.geom.Geometry geometry,
      GeometryType headerType,
      boolean hasZ,
      boolean hasM) {

    GeometryType type = headerType == GeometryType.UNKNOWN ? typeOf(geometry) : headerType;

    // The type is stored on the feature only when the header leaves it open. Repeating a type the
    // header already fixes would cost a field on every feature of the file.
    int storedType =
        headerType == GeometryType.UNKNOWN ? type.value() : GeometryType.UNKNOWN.value();

    if (type == GeometryType.MULTIPOLYGON) {
      MultiPolygon multiPolygon = (MultiPolygon) geometry;
      int[] parts = new int[multiPolygon.getNumGeometries()];
      for (int i = 0; i < parts.length; i++) {
        parts[i] = writeSimple(builder, multiPolygon.getGeometryN(i), GeometryType.POLYGON,
            GeometryType.POLYGON.value(), hasZ, hasM);
      }
      int partsOffset = Geometry.createPartsVector(builder, parts);
      return Geometry.createGeometry(builder, 0, 0, 0, 0, 0, 0, storedType, partsOffset);
    }

    return writeSimple(builder, geometry, type, storedType, hasZ, hasM);
  }

  private static int writeSimple(
      FlatBufferBuilder builder,
      org.locationtech.jts.geom.Geometry geometry,
      GeometryType type,
      int storedType,
      boolean hasZ,
      boolean hasM) {

    long[] ends = ends(geometry, type);
    int numPoints = geometry.getNumPoints();

    // The ordinates are fed through a CoordinateSequenceFilter rather than through
    // getCoordinates(), which would allocate a Coordinate array per geometry.
    Geometry.startXyVector(builder, 2 * numPoints);
    apply(geometry, new OrdinateWriter(builder, CoordinateSequence.Y, CoordinateSequence.X));
    int xyOffset = builder.endVector();

    int zOffset = 0;
    if (hasZ) {
      Geometry.startZVector(builder, numPoints);
      apply(geometry, new OrdinateWriter(builder, CoordinateSequence.Z));
      zOffset = builder.endVector();
    }

    int mOffset = 0;
    if (hasM) {
      Geometry.startMVector(builder, numPoints);
      apply(geometry, new OrdinateWriter(builder, CoordinateSequence.M));
      mOffset = builder.endVector();
    }

    int endsOffset = ends == null ? 0 : Geometry.createEndsVector(builder, ends);
    return Geometry.createGeometry(builder, endsOffset, xyOffset, zOffset, mOffset, 0, 0,
        storedType, 0);
  }

  /**
   * Returns the index at which each ring or line of {@code geometry} ends, or null for a geometry
   * made of a single run of coordinates, which needs no such index.
   */
  private static long[] ends(org.locationtech.jts.geom.Geometry geometry, GeometryType type) {
    if (type == GeometryType.POLYGON) {
      Polygon polygon = (Polygon) geometry;
      long[] ends = new long[polygon.getNumInteriorRing() + 1];
      int end = polygon.getExteriorRing().getNumPoints();
      ends[0] = end;
      for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
        end += polygon.getInteriorRingN(i).getNumPoints();
        ends[i + 1] = end;
      }
      return ends;
    }
    if (type == GeometryType.MULTILINESTRING && geometry.getNumGeometries() > 1) {
      long[] ends = new long[geometry.getNumGeometries()];
      int end = 0;
      for (int i = 0; i < ends.length; i++) {
        end += geometry.getGeometryN(i).getNumPoints();
        ends[i] = end;
      }
      return ends;
    }
    return null;
  }

  /**
   * Applies {@code filter} to the coordinate sequences of {@code geometry} in reverse order, the
   * exterior ring of a polygon coming after its holes.
   * <p>
   * FlatBuffers builds a vector from its end towards its start, so everything written into one has
   * to be visited backwards.
   */
  private static void apply(org.locationtech.jts.geom.Geometry geometry,
      CoordinateSequenceFilter filter) {
    if (geometry.getNumGeometries() > 1) {
      for (int i = geometry.getNumGeometries() - 1; i >= 0; i--) {
        apply(geometry.getGeometryN(i), filter);
      }
    } else if (geometry instanceof Polygon polygon) {
      for (int i = polygon.getNumInteriorRing() - 1; i >= 0; i--) {
        apply(polygon.getInteriorRingN(i), filter);
      }
      apply(polygon.getExteriorRing(), filter);
    } else {
      geometry.apply(filter);
    }
  }

  /**
   * Writes the given ordinates of every coordinate of a sequence, back to front. The ordinates of a
   * single coordinate are reversed too, so an xy vector is fed Y before X.
   */
  private static final class OrdinateWriter implements CoordinateSequenceFilter {

    private final FlatBufferBuilder builder;
    private final int[] ordinates;

    private OrdinateWriter(FlatBufferBuilder builder, int... ordinates) {
      this.builder = builder;
      this.ordinates = ordinates;
    }

    @Override
    public void filter(CoordinateSequence sequence, int index) {
      int reversed = sequence.size() - index - 1;
      for (int ordinate : ordinates) {
        builder.addDouble(sequence.getOrdinate(reversed, ordinate));
      }
    }

    @Override
    public boolean isGeometryChanged() {
      return false;
    }

    @Override
    public boolean isDone() {
      return false;
    }
  }

  /**
   * Reads {@code geometry}, whose type comes from the header unless the header leaves it open, in
   * which case the feature carries it.
   */
  static org.locationtech.jts.geom.Geometry read(
      GeometryFactory factory,
      Geometry geometry,
      GeometryType headerType) {

    GeometryType type =
        headerType == GeometryType.UNKNOWN ? GeometryType.of(geometry.type()) : headerType;

    if (type == GeometryType.MULTIPOLYGON) {
      Polygon[] polygons = new Polygon[geometry.partsLength()];
      for (int i = 0; i < polygons.length; i++) {
        polygons[i] = (Polygon) read(factory, geometry.parts(i), GeometryType.POLYGON);
      }
      return factory.createMultiPolygon(polygons);
    }

    Coordinate[] coordinates = coordinates(geometry);
    return switch (type) {
      case POINT -> coordinates.length == 0
          ? factory.createPoint()
          : factory.createPoint(coordinates[0]);
      case MULTIPOINT -> factory.createMultiPointFromCoords(coordinates);
      case LINESTRING -> factory.createLineString(coordinates);
      case MULTILINESTRING -> factory.createMultiLineString(lines(factory, geometry, coordinates));
      case POLYGON -> polygon(factory, geometry, coordinates);
      default -> throw new IllegalArgumentException("Unsupported geometry type: " + type);
    };
  }

  private static Coordinate[] coordinates(Geometry geometry) {
    int numPoints = geometry.xyLength() / 2;
    int zLength = geometry.zLength();
    int mLength = geometry.mLength();
    Coordinate[] coordinates = new Coordinate[numPoints];
    for (int i = 0; i < numPoints; i++) {
      double x = geometry.xy(2 * i);
      double y = geometry.xy(2 * i + 1);
      double z = i < zLength ? geometry.z(i) : Coordinate.NULL_ORDINATE;
      coordinates[i] = mLength == 0
          ? new Coordinate(x, y, z)
          : new CoordinateXYZM(x, y, z,
              i < mLength ? geometry.m(i) : Coordinate.NULL_ORDINATE);
    }
    return coordinates;
  }

  private static Polygon polygon(GeometryFactory factory, Geometry geometry,
      Coordinate[] coordinates) {
    if (geometry.endsLength() <= 1) {
      return factory.createPolygon(coordinates);
    }
    LinearRing[] rings = new LinearRing[geometry.endsLength()];
    int start = 0;
    for (int i = 0; i < rings.length; i++) {
      int end = (int) geometry.ends(i);
      rings[i] = factory.createLinearRing(Arrays.copyOfRange(coordinates, start, end));
      start = end;
    }
    return factory.createPolygon(rings[0], Arrays.copyOfRange(rings, 1, rings.length));
  }

  private static LineString[] lines(GeometryFactory factory, Geometry geometry,
      Coordinate[] coordinates) {
    if (geometry.endsLength() < 2) {
      return new LineString[] {factory.createLineString(coordinates)};
    }
    LineString[] lines = new LineString[geometry.endsLength()];
    int start = 0;
    for (int i = 0; i < lines.length; i++) {
      int end = (int) geometry.ends(i);
      lines[i] = factory.createLineString(Arrays.copyOfRange(coordinates, start, end));
      start = end;
    }
    return lines;
  }

  private static GeometryType typeOf(org.locationtech.jts.geom.Geometry geometry) {
    if (geometry instanceof Point) {
      return GeometryType.POINT;
    } else if (geometry instanceof MultiPoint) {
      return GeometryType.MULTIPOINT;
    } else if (geometry instanceof LineString) {
      return GeometryType.LINESTRING;
    } else if (geometry instanceof MultiLineString) {
      return GeometryType.MULTILINESTRING;
    } else if (geometry instanceof Polygon) {
      return GeometryType.POLYGON;
    } else if (geometry instanceof MultiPolygon) {
      return GeometryType.MULTIPOLYGON;
    } else {
      throw new IllegalArgumentException(
          "Unsupported geometry: " + geometry.getGeometryType());
    }
  }
}
