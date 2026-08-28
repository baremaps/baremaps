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

import com.baremaps.flatgeobuf.generated.Geometry;
import com.baremaps.flatgeobuf.generated.GeometryType;
import com.google.flatbuffers.FlatBufferBuilder;
import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import org.locationtech.jts.geom.*;

/**
 * This code has been adapted from FlatGeoBuf (BSD 2-Clause "Simplified" License).
 * <p>
 * Copyright (c) 2018, Björn Harrtell
 */
public class GeometryConversions {

  private static final String UNKNOWN_GEOMETRY_TYPE = "Unknown geometry type";

  private GeometryConversions() {
    // Prevent instantiation
  }

  @SuppressWarnings("squid:S3776")
  public static GeometryOffsets writeGeometryPart(
      FlatBufferBuilder builder,
      org.locationtech.jts.geom.Geometry geometry,
      int geometryType) {

    GeometryOffsets go = new GeometryOffsets();

    if (geometry == null) {
      return go;
    }

    if (geometryType == GeometryType.MultiLineString) {
      int end = 0;
      MultiLineString mls = (MultiLineString) geometry;
      if (mls.getNumGeometries() > 1) {
        go.ends = new long[mls.getNumGeometries()];
        for (int i = 0; i < mls.getNumGeometries(); i++) {
          end += mls.getGeometryN(i).getNumPoints();
          go.ends[i] = end;
        }
      }
    } else if (geometryType == GeometryType.Polygon) {
      Polygon p = (Polygon) geometry;
      go.ends = new long[p.getNumInteriorRing() + 1];
      int end = p.getExteriorRing().getNumPoints();
      go.ends[0] = end;
      for (int i = 0; i < p.getNumInteriorRing(); i++) {
        end += p.getInteriorRingN(i).getNumPoints();
        go.ends[i + 1] = end;
      }
    } else if (geometryType == GeometryType.MultiPolygon) {
      MultiPolygon mp = (MultiPolygon) geometry;
      int numGeometries = mp.getNumGeometries();
      GeometryOffsets[] gos = new GeometryOffsets[numGeometries];
      for (int i = 0; i < numGeometries; i++) {
        Polygon p = (Polygon) mp.getGeometryN(i);
        gos[i] = writeGeometryPart(builder, p, GeometryType.Polygon);
      }
      go.gos = gos;
      return go;
    }

    final int numPoints = geometry.getNumPoints();
    // build the vector "manually", using a CoordinateSequenceFilter to avoid creating
    // Coordinate arrays or any Coordinate at all, depending on the underlying
    // CoordinateSequence implementation. Vector elements ought to be added in reverse order
    Geometry.startXyVector(builder, 2 * numPoints);
    ReverseXYCoordinateSequenceFilter filter = new ReverseXYCoordinateSequenceFilter(builder);
    applyInReverseOrder(geometry, filter);
    go.coordsOffset = builder.endVector();

    if (filter.hasZ) {
      Geometry.startZVector(builder, numPoints);
      applyInReverseOrder(geometry, new OrdinateCoordinateSequenceFilter(builder, 2));
      go.zOffset = builder.endVector();
    } else {
      go.zOffset = 0;
    }

    if (filter.hasM) {
      Geometry.startMVector(builder, numPoints);
      applyInReverseOrder(geometry, new OrdinateCoordinateSequenceFilter(builder, 3));
      go.mOffset = builder.endVector();
    } else {
      go.mOffset = 0;
    }

    if (go.ends != null) {
      go.endsOffset = Geometry.createEndsVector(builder, go.ends);
    }

    go.type = geometryType;

    return go;
  }

  public static int writeGeometry(FlatBufferBuilder builder,
      org.locationtech.jts.geom.Geometry geometry, byte geometryType) {
    byte knownGeometryType = geometryType;
    if (geometryType == GeometryType.Unknown) {
      knownGeometryType = GeometryConversions.toGeometryType(geometry.getClass());
    }

    GeometryOffsets go =
        GeometryConversions.writeGeometryPart(builder, geometry, knownGeometryType);

    int geometryOffset;
    if (go.gos != null && go.gos.length > 0) {
      int[] partOffsets = new int[go.gos.length];
      for (int i = 0; i < go.gos.length; i++) {
        GeometryOffsets goPart = go.gos[i];
        int partOffset = Geometry.createGeometry(builder, goPart.endsOffset,
            goPart.coordsOffset, goPart.zOffset, goPart.mOffset, 0, 0, goPart.type, 0);
        partOffsets[i] = partOffset;
      }
      int partsOffset = Geometry.createPartsVector(builder, partOffsets);
      geometryOffset = Geometry.createGeometry(builder, 0, 0, 0, 0, 0, 0,
          geometryType == GeometryType.Unknown ? knownGeometryType : 0, partsOffset);
    } else {
      geometryOffset =
          Geometry.createGeometry(builder, go.endsOffset, go.coordsOffset, go.zOffset, go.mOffset,
              0, 0, geometryType == GeometryType.Unknown ? knownGeometryType : 0, 0);
    }
    return geometryOffset;
  }

  /**
   * Applies the {@code filter} to all {@link org.locationtech.jts.geom.Geometry#getGeometryN(int)
   * subgeometries} in reverse order if it's a {@link GeometryCollection} or a {@link Polygon} (i.e.
   * interior rings in reverse order first)
   */
  private static void applyInReverseOrder(org.locationtech.jts.geom.Geometry geometry,
      CoordinateSequenceFilter filter) {

    final int numGeometries = geometry.getNumGeometries();
    if (numGeometries > 1) {
      for (int i = numGeometries - 1; i >= 0; i--) {
        org.locationtech.jts.geom.Geometry sub = geometry.getGeometryN(i);
        applyInReverseOrder(sub, filter);
      }
    } else if (geometry instanceof Polygon polygon) {
      for (int i = polygon.getNumInteriorRing() - 1; i >= 0; i--) {
        org.locationtech.jts.geom.Geometry hole = polygon.getInteriorRingN(i);
        applyInReverseOrder(hole, filter);
      }
      applyInReverseOrder(polygon.getExteriorRing(), filter);
    } else {
      geometry.apply(filter);
    }
  }

  private static class OrdinateCoordinateSequenceFilter implements CoordinateSequenceFilter {
    private FlatBufferBuilder builder;
    private final int ordinateIndex;

    OrdinateCoordinateSequenceFilter(FlatBufferBuilder builder, int ordinateIndex) {
      this.builder = builder;
      this.ordinateIndex = ordinateIndex;
    }

    public @Override void filter(final CoordinateSequence seq, final int coordIndex) {
      int reverseSeqIndex = seq.size() - coordIndex - 1;
      builder.addDouble(seq.getOrdinate(reverseSeqIndex, ordinateIndex));
    }

    public @Override boolean isGeometryChanged() {
      return false;
    }

    public @Override boolean isDone() {
      return false;
    }
  }

  private static class ReverseXYCoordinateSequenceFilter implements CoordinateSequenceFilter {
    private FlatBufferBuilder builder;
    boolean hasZ = false;
    boolean hasM = false;

    ReverseXYCoordinateSequenceFilter(FlatBufferBuilder builder) {
      this.builder = builder;
    }

    public @Override void filter(final CoordinateSequence seq, final int coordIndex) {
      int reverseSeqIndex = seq.size() - coordIndex - 1;
      double y = seq.getOrdinate(reverseSeqIndex, 1);
      double x = seq.getOrdinate(reverseSeqIndex, 0);
      builder.addDouble(y);
      builder.addDouble(x);
      if (!hasZ && seq.hasZ()) {
        hasZ = true;
      }
      if (!hasM && seq.hasM()) {
        hasM = true;
      }
    }

    public boolean isHasZ() {
      return hasZ;
    }

    public boolean isHasM() {
      return hasM;
    }

    public @Override boolean isGeometryChanged() {
      return false;
    }

    public @Override boolean isDone() {
      return false;
    }
  }

  @SuppressWarnings("squid:S3776")
  public static org.locationtech.jts.geom.Geometry readGeometry(Geometry geometry,
      int geometryType) {
    GeometryFactory factory = new GeometryFactory();

    if (geometryType == GeometryType.MultiPolygon) {
      int partsLength = geometry.partsLength();
      Polygon[] polygons = new Polygon[partsLength];
      for (int i = 0; i < geometry.partsLength(); i++) {
        polygons[i] = (Polygon) readGeometry(geometry.parts(i), GeometryType.Polygon);
      }
      return factory.createMultiPolygon(polygons);
    }

    int xyLength = geometry.xyLength();

    Coordinate[] coordinates = new Coordinate[xyLength >> 1];

    int c = 0;
    for (int i = 0; i < xyLength; i = i + 2) {
      if (c < geometry.mLength()) {
        coordinates[c++] = new CoordinateXYZM(geometry.xy(i), geometry.xy(i + 1),
            (i >> 1) < geometry.zLength() ? geometry.z((i >> 1)) : Coordinate.NULL_ORDINATE,
            (i >> 1) < geometry.mLength() ? geometry.m((i >> 1)) : Coordinate.NULL_ORDINATE);
      } else {
        coordinates[c++] = new Coordinate(geometry.xy(i), geometry.xy(i + 1),
            (i >> 1) < geometry.zLength() ? geometry.z((i >> 1)) : Coordinate.NULL_ORDINATE);
      }
    }

    IntFunction<Polygon> makePolygonWithRings = (int endsLength) -> {
      LinearRing[] lrs = new LinearRing[endsLength];
      int s = 0;
      for (int i = 0; i < endsLength; i++) {
        int e = (int) geometry.ends(i);
        Coordinate[] cs = Arrays.copyOfRange(coordinates, s, e);
        lrs[i] = factory.createLinearRing(cs);
        s = e;
      }
      LinearRing shell = lrs[0];
      LinearRing[] holes = Arrays.copyOfRange(lrs, 1, endsLength);
      return factory.createPolygon(shell, holes);
    };

    Supplier<Polygon> makePolygon = () -> {
      int endsLength = geometry.endsLength();
      if (endsLength > 1) {
        return makePolygonWithRings.apply(endsLength);
      } else {
        return factory.createPolygon(coordinates);
      }
    };

    switch (geometryType) {
      case GeometryType.Unknown:
        return null;
      case GeometryType.Point:
        if (coordinates.length > 0) {
          return factory.createPoint(coordinates[0]);
        } else {
          return factory.createPoint();
        }
      case GeometryType.MultiPoint:
        return factory.createMultiPointFromCoords(coordinates);
      case GeometryType.LineString:
        return factory.createLineString(coordinates);
      case GeometryType.MultiLineString: {
        int lengthLengths = geometry.endsLength();
        if (lengthLengths < 2) {
          return factory
              .createMultiLineString(new LineString[] {factory.createLineString(coordinates)});
        }
        LineString[] lss = new LineString[lengthLengths];
        int s = 0;
        for (int i = 0; i < lengthLengths; i++) {
          int e = (int) geometry.ends(i);
          Coordinate[] cs = Arrays.copyOfRange(coordinates, s, e);
          lss[i] = factory.createLineString(cs);
          s = e;
        }
        return factory.createMultiLineString(lss);
      }
      case GeometryType.Polygon:
        return makePolygon.get();
      default:
        throw new IllegalArgumentException(UNKNOWN_GEOMETRY_TYPE);
    }
  }

  public static Class<?> getGeometryClass(int geometryType) {
    switch (geometryType) {
      case GeometryType.Unknown:
        return Geometry.class;
      case GeometryType.Point:
        return Point.class;
      case GeometryType.MultiPoint:
        return MultiPoint.class;
      case GeometryType.LineString:
        return LineString.class;
      case GeometryType.MultiLineString:
        return MultiLineString.class;
      case GeometryType.Polygon:
        return Polygon.class;
      case GeometryType.MultiPolygon:
        return MultiPolygon.class;
      default:
        throw new IllegalArgumentException(UNKNOWN_GEOMETRY_TYPE);
    }
  }

  public static byte toGeometryType(Class<?> geometryClass) {
    if (geometryClass == org.locationtech.jts.geom.Geometry.class) {
      return GeometryType.Unknown;
    } else if (MultiPoint.class.isAssignableFrom(geometryClass)) {
      return GeometryType.MultiPoint;
    } else if (Point.class.isAssignableFrom(geometryClass)) {
      return GeometryType.Point;
    } else if (MultiLineString.class.isAssignableFrom(geometryClass)) {
      return GeometryType.MultiLineString;
    } else if (LineString.class.isAssignableFrom(geometryClass)) {
      return GeometryType.LineString;
    } else if (MultiPolygon.class.isAssignableFrom(geometryClass)) {
      return GeometryType.MultiPolygon;
    } else if (Polygon.class.isAssignableFrom(geometryClass)) {
      return GeometryType.Polygon;
    } else {
      throw new IllegalArgumentException(UNKNOWN_GEOMETRY_TYPE);
    }
  }

  public static class GeometryOffsets {

    private int coordsOffset;
    private int zOffset;
    private int mOffset;
    private long[] ends = null;
    private int[] lengths = null;
    private int endsOffset = 0;
    private int lengthsOffset = 0;
    private int type = 0;
    private GeometryOffsets[] gos = null;

    public GeometryOffsets() {
      // Default constructor
    }

    public int getCoordsOffset() {
      return coordsOffset;
    }

    public int getzOffset() {
      return zOffset;
    }

    public int getmOffset() {
      return mOffset;
    }

    public long[] getEnds() {
      return ends;
    }

    public int[] getLengths() {
      return lengths;
    }

    public int getEndsOffset() {
      return endsOffset;
    }

    public int getLengthsOffset() {
      return lengthsOffset;
    }

    public int getType() {
      return type;
    }

    public GeometryOffsets[] getGos() {
      return gos;
    }

    public void setCoordsOffset(int coordsOffset) {
      this.coordsOffset = coordsOffset;
    }

    public void setzOffset(int zOffset) {
      this.zOffset = zOffset;
    }

    public void setmOffset(int mOffset) {
      this.mOffset = mOffset;
    }

    public void setEnds(long[] ends) {
      this.ends = ends;
    }

    public void setLengths(int[] lengths) {
      this.lengths = lengths;
    }

    public void setEndsOffset(int endsOffset) {
      this.endsOffset = endsOffset;
    }

    public void setLengthsOffset(int lengthsOffset) {
      this.lengthsOffset = lengthsOffset;
    }

    public void setType(int type) {
      this.type = type;
    }

    public void setGos(GeometryOffsets[] gos) {
      this.gos = gos;
    }
  }
}
