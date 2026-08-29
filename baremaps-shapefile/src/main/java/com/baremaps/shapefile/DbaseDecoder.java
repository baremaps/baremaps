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

import com.baremaps.shapefile.Shapefile.Column;
import com.baremaps.shapefile.Shapefile.ColumnType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Decodes the records of the dBASE table that holds the attributes of the features, in the order
 * the table stores them.
 * <p>
 * Every field is read within the width its column declares, so a value this decoder reads short of,
 * or does not decode at all, cannot shift the fields that follow it onto the wrong columns.
 */
class DbaseDecoder {

  /**
   * A record of the table.
   *
   * @param deleted whether the record is marked as deleted, in which case it carries no values. A
   *        deleted record stays in the file, and the {@code .shp} file keeps a geometry for it, so
   *        a caller pairing the two has to account for it rather than skip past it unaware.
   * @param values one value per {@link #columns() column}, in column order
   */
  record Row(boolean deleted, List<Object> values) {
  }

  /** Marks the end of the column descriptors. */
  private static final byte TERMINATOR = 0x0D;

  /** Marks a record as deleted, in the first byte of the record. */
  private static final byte DELETED = '*';

  /** The fixed part of the header, which the column descriptors follow. */
  private static final int HEADER_SIZE = 32;

  /** The size of one column descriptor. */
  private static final int COLUMN_SIZE = 32;

  /** The width of the name of a column, padded with spaces or with zeroes. */
  private static final int NAME_SIZE = 11;

  /** The Julian day the epoch of {@link LocalDate} falls on. */
  private static final int JULIAN_DAY_OF_EPOCH = 2440588;

  private final ByteBuffer buffer;

  private final Charset charset;

  private final List<Column> columns;

  /** The offset of the first record, which the header states as its own length. */
  private final int firstRecord;

  private final int recordLength;

  private final int rowCount;

  private int rowsRead;

  /**
   * @param charset the charset of the text of the table, or null to take the one its code page
   *        stands for
   */
  DbaseDecoder(ByteBuffer buffer, Charset charset) throws ShapefileException {
    this.buffer = buffer;
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    if (buffer.capacity() < HEADER_SIZE) {
      throw new ShapefileException("The table is shorter than its own header");
    }

    int declaredRows = buffer.getInt(4);
    this.firstRecord = Short.toUnsignedInt(buffer.getShort(8));
    this.recordLength = Short.toUnsignedInt(buffer.getShort(10));
    if (recordLength == 0 || firstRecord < HEADER_SIZE || firstRecord > buffer.capacity()) {
      throw new ShapefileException(
          "The table declares a header of %d bytes and records of %d bytes, which describe no "
              .formatted(firstRecord, recordLength) + "readable table");
    }

    this.charset = charset != null ? charset : codePage(buffer.get(29));
    this.columns = readColumns();

    // The header states how many records the table holds, but a writer that interrupted itself
    // leaves behind a count larger than the file, so the file itself caps it.
    int storable = (buffer.capacity() - firstRecord) / recordLength;
    this.rowCount = Math.min(Math.max(declaredRows, 0), storable);
  }

  List<Column> columns() {
    return columns;
  }

  /** Decodes the next record, or returns null once the table holds no further one. */
  Row next() {
    if (rowsRead >= rowCount) {
      return null;
    }
    int record = firstRecord + rowsRead * recordLength;
    rowsRead++;

    if (buffer.get(record) == DELETED) {
      return new Row(true, null);
    }

    List<Object> values = new ArrayList<>(columns.size());
    int field = record + 1;
    for (Column column : columns) {
      buffer.position(field);
      values.add(value(column));
      field += column.length();
    }
    return new Row(false, values);
  }

  private List<Column> readColumns() throws ShapefileException {
    List<Column> columns = new ArrayList<>();
    for (int offset = HEADER_SIZE; offset < firstRecord; offset += COLUMN_SIZE) {
      if (buffer.get(offset) == TERMINATOR) {
        return List.copyOf(columns);
      }
      if (offset + COLUMN_SIZE > firstRecord) {
        break;
      }
      columns.add(readColumn(offset));
    }
    // Without the terminator there is no telling where the descriptors end and the records begin.
    throw new ShapefileException("The columns of the table are not terminated by 0x0D");
  }

  private Column readColumn(int offset) throws ShapefileException {
    byte[] name = new byte[NAME_SIZE];
    buffer.get(offset, name);
    char code = (char) Byte.toUnsignedInt(buffer.get(offset + 11));
    try {
      return new Column(
          text(name, name.length),
          ColumnType.of(code),
          Byte.toUnsignedInt(buffer.get(offset + 16)),
          Byte.toUnsignedInt(buffer.get(offset + 17)));
    } catch (IllegalArgumentException e) {
      throw new ShapefileException(
          "Column %s has the unsupported type '%c'".formatted(text(name, name.length), code), e);
    }
  }

  private Object value(Column column) {
    return switch (column.type()) {
      case CHARACTER, MEMO, PICTURE, VARI_FIELD, VARIANT -> text(column.length());
      case NUMBER -> column.decimalCount() == 0
          ? parse(text(column.length()), Long::parseLong)
          : parse(text(column.length()), Double::parseDouble);
      case FLOATING_POINT -> parse(text(column.length()), Double::parseDouble);
      case LOGICAL -> logical(text(column.length()));
      case DATE -> parse(text(column.length()), DbaseDecoder::date);
      // The types below are written as little-endian binary rather than as text.
      case INTEGER, AUTO_INCREMENT -> buffer.getInt();
      case DOUBLE -> buffer.getDouble();
      // An amount is stored as an integer of four implied decimals.
      case CURRENCY -> buffer.getLong() / 10_000d;
      case DATE_TIME, TIMESTAMP -> timestamp();
    };
  }

  /** Reads the next {@code length} bytes of the record as text. */
  private String text(int length) {
    byte[] field = new byte[length];
    buffer.get(field);
    return text(field, length);
  }

  /**
   * Decodes the first {@code length} bytes as text, without the padding the table adds on the
   * right. Spaces and zeroes both serve as padding, and neither carries a meaning of its own.
   */
  private String text(byte[] field, int length) {
    int end = length;
    while (end > 0 && Byte.toUnsignedInt(field[end - 1]) <= ' ') {
      end--;
    }
    return new String(field, 0, end, charset);
  }

  /**
   * Converts the text of a field, or returns null when the field is blank or holds something its
   * type does not describe. Tables in the wild pad an unset number with spaces and an overflowed
   * one with asterisks, so text that fails to convert is an ordinary occurrence rather than a sign
   * of a corrupt file.
   */
  private static <T> T parse(String text, Function<String, T> parser) {
    String value = text.strip();
    if (value.isEmpty()) {
      return null;
    }
    try {
      return parser.apply(value);
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static LocalDate date(String text) {
    return LocalDate.parse(text, DateTimeFormatter.BASIC_ISO_DATE);
  }

  private static Boolean logical(String text) {
    return switch (text.isEmpty() ? ' ' : text.charAt(0)) {
      case 'Y', 'y', 'T', 't' -> Boolean.TRUE;
      case 'N', 'n', 'F', 'f' -> Boolean.FALSE;
      // A question mark, like a blank, stands for a record that does not decide.
      default -> null;
    };
  }

  /**
   * Reads the eight bytes of a time stamp: the Julian day of the date, then the milliseconds
   * elapsed since midnight. Day zero stands for an unset value rather than for a date.
   */
  private LocalDateTime timestamp() {
    int day = buffer.getInt();
    int millis = buffer.getInt();
    return day == 0
        ? null
        : LocalDate.ofEpochDay(day - (long) JULIAN_DAY_OF_EPOCH).atStartOfDay()
            .plusNanos(millis * 1_000_000L);
  }

  /**
   * The charset the code page of the table stands for, or ISO-8859-1 when it names none. Latin-1 is
   * what the tools that write shapefiles assume in that case and, unlike UTF-8, it maps every byte
   * to a character, so an attribute of an unknown encoding is mangled rather than lost.
   *
   * @see <a href="http://trac.osgeo.org/gdal/ticket/2864">the GDAL ticket this table comes from</a>
   */
  private static Charset codePage(byte code) {
    String name = switch (Byte.toUnsignedInt(code)) {
      case 0x01, 0x09, 0x0b, 0x0d, 0x0f, 0x11, 0x15, 0x18, 0x19, 0x1b -> "cp437";
      case 0x02, 0x0a, 0x0e, 0x10, 0x12, 0x14, 0x16, 0x1a, 0x1d, 0x25, 0x37 -> "cp850";
      case 0x03, 0x57, 0x58, 0x59 -> "cp1252";
      case 0x08, 0x17, 0x66 -> "cp865";
      case 0x13, 0x7b -> "cp932";
      case 0x1c, 0x6c -> "cp863";
      case 0x1f, 0x22, 0x23, 0x40, 0x64, 0x87 -> "cp852";
      case 0x24 -> "cp860";
      case 0x26, 0x65 -> "cp866";
      case 0x4d, 0x7a -> "cp936";
      case 0x4e, 0x79 -> "cp949";
      case 0x4f, 0x78 -> "cp950";
      case 0x50, 0x7c -> "cp874";
      case 0x67 -> "cp861";
      case 0x6a, 0x86 -> "cp737";
      case 0x6b, 0x88 -> "cp857";
      case 0xc8 -> "cp1250";
      case 0xc9 -> "cp1251";
      case 0xca -> "cp1254";
      case 0xcb -> "cp1253";
      case 0xcc -> "cp1257";
      default -> null;
    };
    try {
      return name == null ? StandardCharsets.ISO_8859_1 : Charset.forName(name);
    } catch (IllegalArgumentException e) {
      // A runtime built without the legacy charsets still reads the ASCII range of the table.
      return StandardCharsets.ISO_8859_1;
    }
  }
}
