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

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.locationtech.jts.geom.Envelope;

/**
 * The domain model of a shapefile, as read by {@link ShapefileReader}. It mirrors the ESRI
 * specification and the dBASE III table that carries the attributes, without exposing either
 * encoding: the byte layouts are a detail of the reader.
 *
 * @see <a href=
 *      "https://www.esri.com/content/dam/esrisites/sitecore-archive/Files/Pdfs/library/whitepapers/pdfs/shapefile.pdf">ESRI
 *      Shapefile Technical Description</a>
 * @see <a href="http://www.clicketyclick.dk/databases/xbase/format/data_types.html">Xbase data
 *      types</a>
 */
public final class Shapefile {

  private Shapefile() {
    // Prevent instantiation
  }

  /**
   * The geometry types of the specification. The wire value is declared explicitly so that the
   * order of the constants stays a matter of readability rather than of file compatibility.
   * <p>
   * The Z and M variants of a type share the layout of their plain counterpart and append their
   * extra ordinates after it, which is why the reader decodes them as their two-dimensional
   * projection instead of rejecting them.
   */
  public enum GeometryType {

    NULL(0),
    POINT(1),
    POLYLINE(3),
    POLYGON(5),
    MULTIPOINT(8),
    POINT_Z(11),
    POLYLINE_Z(13),
    POLYGON_Z(15),
    MULTIPOINT_Z(18),
    POINT_M(21),
    POLYLINE_M(23),
    POLYGON_M(25),
    MULTIPOINT_M(28),
    MULTIPATCH(31);

    private static final GeometryType[] BY_VALUE = new GeometryType[MULTIPATCH.value + 1];

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
      GeometryType type = value < 0 || value >= BY_VALUE.length ? null : BY_VALUE[value];
      if (type == null) {
        throw new IllegalArgumentException("Unsupported geometry type: " + value);
      }
      return type;
    }
  }

  /**
   * The column types of the dBASE table, identified by the character the table stores for them.
   * <p>
   * The types fall in two families that the reader decodes differently: most are written as
   * fixed-width text, but {@link #INTEGER}, {@link #DOUBLE}, {@link #CURRENCY},
   * {@link #AUTO_INCREMENT}, {@link #DATE_TIME} and {@link #TIMESTAMP} are written as little-endian
   * binary.
   */
  public enum ColumnType {

    /** Text of at most 254 characters. */
    CHARACTER('C'),
    /** A number written as text, with a sign and a decimal point. */
    NUMBER('N'),
    /** A three-way flag: true, false, or undecided. */
    LOGICAL('L'),
    /** A date written as text, in the YYYYMMDD format. */
    DATE('D'),
    /** A pointer into the memo file that accompanies the table. */
    MEMO('M'),
    /** A floating point number written as text. */
    FLOATING_POINT('F'),
    /** A picture, stored like a memo. */
    PICTURE('P'),
    /** A binary amount with four implied decimals. */
    CURRENCY('Y'),
    /** A binary Julian day followed by binary milliseconds since midnight. */
    DATE_TIME('T'),
    /** A binary four byte integer. */
    INTEGER('I'),
    /** A variable length field of an undocumented layout. */
    VARI_FIELD('V'),
    /** A variant of an undocumented layout. */
    VARIANT('X'),
    /** A time stamp, laid out like {@link #DATE_TIME}. */
    TIMESTAMP('@'),
    /** A binary eight byte floating point number. */
    DOUBLE('O'),
    /** A binary four byte integer maintained by the table. */
    AUTO_INCREMENT('+');

    private final char code;

    ColumnType(char code) {
      this.code = code;
    }

    public char code() {
      return code;
    }

    public static ColumnType of(char code) {
      for (ColumnType type : values()) {
        if (type.code == code) {
          return type;
        }
      }
      throw new IllegalArgumentException("Unsupported column type: " + code);
    }
  }

  /**
   * A column of the dBASE table that holds the attributes of the features.
   *
   * @param name the name of the column
   * @param type the type the table declares for it
   * @param length the width in bytes of the column in every record
   * @param decimalCount the number of decimals of a {@link ColumnType#NUMBER}, zero otherwise
   */
  public record Column(String name, ColumnType type, int length, int decimalCount) {

    /**
     * The Java type of the values {@link ShapefileReader#readRow()} produces for this column. It is
     * declared here rather than by each caller so that a mapping onto another type system, such as
     * the SQL types of a table, cannot drift from what the reader actually returns. Any value may
     * additionally be null, which is how the table encodes a blank field.
     */
    public Class<?> javaType() {
      return switch (type) {
        case CHARACTER, MEMO, PICTURE, VARI_FIELD, VARIANT -> String.class;
        // A number without decimals is an identifier or a count more often than not, and the ten
        // digits that shapefiles commonly declare for one overflow an int.
        case NUMBER -> decimalCount == 0 ? Long.class : Double.class;
        case FLOATING_POINT, CURRENCY, DOUBLE -> Double.class;
        case INTEGER, AUTO_INCREMENT -> Integer.class;
        case LOGICAL -> Boolean.class;
        case DATE -> LocalDate.class;
        case DATE_TIME, TIMESTAMP -> LocalDateTime.class;
      };
    }
  }

  /**
   * The header of a shapefile, which describes the whole of its content.
   *
   * @param geometryType the type of every geometry in the file
   * @param envelope the bounds of the geometries, which the file states rather than computes and
   *        which is therefore only as accurate as the writer made it
   * @param zMin the lowest Z ordinate, zero when the geometries carry none
   * @param zMax the highest Z ordinate, zero when the geometries carry none
   * @param mMin the lowest measure, zero when the geometries carry none
   * @param mMax the highest measure, zero when the geometries carry none
   */
  public record Header(
      GeometryType geometryType,
      Envelope envelope,
      double zMin,
      double zMax,
      double mMin,
      double mMax) {
  }
}
