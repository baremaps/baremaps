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

import java.nio.ByteBuffer;
import java.util.List;
import org.locationtech.jts.geom.Geometry;

/**
 * This class describes the domain model for FlatGeoBuf.
 *
 * This code has been adapted from FlatGeoBuf (BSD 2-Clause "Simplified" License).
 * <p>
 * Copyright (c) 2018, Björn Harrtell
 */
public class FlatGeoBuf {

  protected static final byte[] MAGIC_BYTES =
      new byte[] {0x66, 0x67, 0x62, 0x03, 0x66, 0x67, 0x62, 0x00};

  private FlatGeoBuf() {
    // Prevent instantiation
  }

  public static boolean isFlatGeoBuf(ByteBuffer bb) {
    return bb.get() == MAGIC_BYTES[0] &&
        bb.get() == MAGIC_BYTES[1] &&
        bb.get() == MAGIC_BYTES[2] &&
        bb.get() == MAGIC_BYTES[3] &&
        bb.get() == MAGIC_BYTES[4] &&
        bb.get() == MAGIC_BYTES[5] &&
        bb.get() == MAGIC_BYTES[6] &&
        bb.get() == MAGIC_BYTES[7];
  }

  // Geometry type enumeration
  public enum GeometryType {
    UNKNOWN(0),
    POINT(1),
    LINESTRING(2),
    POLYGON(3),
    MULTIPOINT(4),
    MULTILINESTRING(5),
    MULTIPOLYGON(6),
    GEOMETRYCOLLECTION(7),
    CIRCULARSTRING(8),
    COMPOUNDCURVE(9),
    CURVEPOLYGON(10),
    MULTICURVE(11),
    MULTISURFACE(12),
    CURVE(13),
    SURFACE(14),
    POLYHEDRALSURFACE(15),
    TIN(16),
    TRIANGLE(17);

    private final int value;

    GeometryType(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public enum ColumnType {
    BYTE,
    UBYTE,
    BOOL,
    SHORT,
    USHORT,
    INT,
    UINT,
    LONG,
    ULONG,
    FLOAT,
    DOUBLE,
    STRING,
    JSON,
    DATETIME,
    BINARY
  }

  public record Column(
      String name,
      ColumnType type,
      String title,
      String description,
      int width,
      int precision,
      int scale,
      boolean nullable,
      boolean unique,
      boolean primaryKey,
      String metadata) {
  }

  public record Crs(
      String org,
      int code,
      String name,
      String description,
      String wkt,
      String codeString) {
  }

  public record Header(
      String name,
      List<Double> envelope,
      GeometryType geometryType,
      boolean hasZ,
      boolean hasM,
      boolean hasT,
      boolean hasTM,
      List<Column> columns,
      long featuresCount,
      int indexNodeSize,
      Crs crs,
      String title,
      String description,
      String metadata) {
  }

  public record Feature(List<Object> properties, Geometry geometry) {
  }
}
