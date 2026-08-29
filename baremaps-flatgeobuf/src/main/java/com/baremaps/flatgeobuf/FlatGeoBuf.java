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

import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * The domain model of a FlatGeoBuf file, as read by {@link FlatGeoBufReader} and written by
 * {@link FlatGeoBufWriter}. It mirrors the FlatGeoBuf schema without exposing FlatBuffers: the
 * generated classes are an encoding detail of the reader and the writer.
 * <p>
 * This code has been adapted from FlatGeoBuf (BSD 2-Clause "Simplified" License).
 * <p>
 * Copyright (c) 2018, Bj&ouml;rn Harrtell
 */
public final class FlatGeoBuf {

  /**
   * The 8 byte file signature. Bytes 0-2 and 4-6 spell "fgb"; byte 3 is the major version of the
   * specification and byte 4 its patch level. A file whose major version differs is deliberately
   * rejected rather than parsed, because the layout it describes is not the one implemented here.
   */
  static final byte[] MAGIC = {0x66, 0x67, 0x62, 0x03, 0x66, 0x67, 0x62, 0x00};

  private FlatGeoBuf() {
    // Prevent instantiation
  }

  /**
   * The geometry types of the specification. The wire value is declared explicitly so that the
   * order of the constants stays a matter of readability rather than of file compatibility.
   */
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

    private static final GeometryType[] BY_VALUE = new GeometryType[TRIANGLE.value + 1];

    static {
      for (GeometryType type : values()) {
        BY_VALUE[type.value] = type;
      }
    }

    private final int value;

    GeometryType(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }

    public static GeometryType of(int value) {
      if (value < 0 || value >= BY_VALUE.length) {
        throw new IllegalArgumentException("Unsupported geometry type: " + value);
      }
      return BY_VALUE[value];
    }
  }

  /**
   * The column types of the specification, with explicit wire values for the same reason as
   * {@link GeometryType}.
   */
  public enum ColumnType {

    BYTE(0),
    UBYTE(1),
    BOOL(2),
    SHORT(3),
    USHORT(4),
    INT(5),
    UINT(6),
    LONG(7),
    ULONG(8),
    FLOAT(9),
    DOUBLE(10),
    STRING(11),
    JSON(12),
    DATETIME(13),
    BINARY(14);

    private static final ColumnType[] BY_VALUE = new ColumnType[BINARY.value + 1];

    static {
      for (ColumnType type : values()) {
        BY_VALUE[type.value] = type;
      }
    }

    private final int value;

    ColumnType(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }

    public static ColumnType of(int value) {
      if (value < 0 || value >= BY_VALUE.length) {
        throw new IllegalArgumentException("Unsupported column type: " + value);
      }
      return BY_VALUE[value];
    }
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

  /**
   * The header of a FlatGeoBuf file. All the reference typed components are optional and may be
   * null; {@code envelope} and {@code crs} are absent from many files in the wild.
   *
   * @param geometryType the type of every geometry in the file, or {@link GeometryType#UNKNOWN}
   *        when the file mixes types and each feature carries its own
   * @param hasZ whether the file stores a Z ordinate; features are written accordingly, so a
   *        geometry with a Z ordinate loses it when the header says otherwise
   */
  public record Header(
      String name,
      Envelope envelope,
      GeometryType geometryType,
      boolean hasZ,
      boolean hasM,
      boolean hasT,
      boolean hasTm,
      List<Column> columns,
      long featuresCount,
      int indexNodeSize,
      Crs crs,
      String title,
      String description,
      String metadata) {
  }

  /**
   * A feature of a FlatGeoBuf file.
   *
   * @param properties one value per {@link Header#columns() header column}, in column order, with
   *        null for the properties the feature does not carry. Keeping the list aligned with the
   *        columns is what lets a feature be read and written back without shifting its values onto
   *        the wrong columns, since the format omits absent properties rather than encoding them as
   *        null.
   */
  public record Feature(List<Object> properties, Geometry geometry) {
  }
}
