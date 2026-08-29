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

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.shapefile.Shapefile.Column;
import com.baremaps.shapefile.Shapefile.ColumnType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DbaseDecoderTest {

  @Test
  void readsTheColumns() throws Exception {
    DbaseDecoder decoder = decoder(
        List.of(column("id", ColumnType.NUMBER, 10, 0), column("name", ColumnType.CHARACTER, 5, 0)),
        row(text("1", 10), text("abc", 5)));

    assertEquals(List.of("id", "name"), decoder.columns().stream().map(Column::name).toList());
    assertEquals(ColumnType.NUMBER, decoder.columns().get(0).type());
    assertEquals(10, decoder.columns().get(0).length());
  }

  @Test
  void readsANumberWithoutDecimalsAsALong() throws Exception {
    DbaseDecoder decoder = decoder(List.of(column("n", ColumnType.NUMBER, 10, 0)),
        row(text("  42", 10)));
    assertEquals(42L, decoder.next().values().get(0));
  }

  @Test
  void readsANumberWithDecimalsAsADouble() throws Exception {
    DbaseDecoder decoder = decoder(List.of(column("n", ColumnType.NUMBER, 10, 2)),
        row(text("3.50", 10)));
    assertEquals(3.5d, decoder.next().values().get(0));
  }

  @Test
  void readsAnUnsetNumberAsNoValue() throws Exception {
    // A blank field and the asterisks of an overflowed one are both ordinary in the wild, and
    // neither is a reason to fail the whole read.
    DbaseDecoder decoder = decoder(List.of(column("n", ColumnType.NUMBER, 10, 0)),
        row(text("", 10)), row(text("**********", 10)));
    assertNull(decoder.next().values().get(0));
    assertNull(decoder.next().values().get(0));
  }

  @Test
  void readsADate() throws Exception {
    DbaseDecoder decoder = decoder(List.of(column("d", ColumnType.DATE, 8, 0)),
        row(text("20231027", 8)), row(text("", 8)), row(text("notadate", 8)));
    assertEquals(LocalDate.of(2023, 10, 27), decoder.next().values().get(0));
    assertNull(decoder.next().values().get(0));
    assertNull(decoder.next().values().get(0));
  }

  @Test
  void readsALogicalAsABoolean() throws Exception {
    DbaseDecoder decoder = decoder(List.of(column("l", ColumnType.LOGICAL, 1, 0)),
        row(text("T", 1)), row(text("f", 1)), row(text("?", 1)));
    assertEquals(Boolean.TRUE, decoder.next().values().get(0));
    assertEquals(Boolean.FALSE, decoder.next().values().get(0));
    assertNull(decoder.next().values().get(0));
  }

  @Test
  void trimsTheTextOfACharacterOnTheRightOnly() throws Exception {
    DbaseDecoder decoder = decoder(List.of(column("c", ColumnType.CHARACTER, 10, 0)),
        row(text("a b  ", 10)));
    assertEquals("a b", decoder.next().values().get(0));
  }

  @Test
  void readsTheBinaryColumnTypes() throws Exception {
    List<Column> columns = List.of(
        column("i", ColumnType.INTEGER, 4, 0),
        column("o", ColumnType.DOUBLE, 8, 0),
        column("y", ColumnType.CURRENCY, 8, 0),
        column("t", ColumnType.DATE_TIME, 8, 0));

    long julianDay = LocalDate.of(2023, 10, 27).toEpochDay() + 2440588;
    byte[] timestamp = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        .putInt((int) julianDay).putInt(3600 * 1000).array();

    DbaseDecoder decoder = decoder(columns, row(
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(7).array(),
        ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(2.5).array(),
        ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(1234567).array(),
        timestamp));

    List<Object> values = decoder.next().values();
    assertEquals(7, values.get(0));
    assertEquals(2.5d, values.get(1));
    // An amount carries four implied decimals.
    assertEquals(123.4567d, values.get(2));
    assertEquals(LocalDateTime.of(2023, 10, 27, 1, 0), values.get(3));
  }

  @Test
  void reportsADeletedRecordRatherThanSkippingIt() throws Exception {
    // The .shp file keeps a geometry for a deleted record, so the reader above has to be told.
    DbaseDecoder decoder = decoder(List.of(column("c", ColumnType.CHARACTER, 3, 0)),
        row(text("aaa", 3)), deletedRow(text("bbb", 3)), row(text("ccc", 3)));

    assertFalse(decoder.next().deleted());
    assertTrue(decoder.next().deleted());
    assertEquals("ccc", decoder.next().values().get(0));
    assertNull(decoder.next());
  }

  @Test
  void takesTheCharsetFromTheCodePageOfTheTable() throws Exception {
    // 0x03 stands for Windows ANSI, where 0x80 is the sign of the euro.
    DbaseDecoder decoder = decoder(0x03, null, List.of(column("c", ColumnType.CHARACTER, 1, 0)),
        row(new byte[] {(byte) 0x80}));
    assertEquals("€", decoder.next().values().get(0));
  }

  @Test
  void fallsBackOnLatin1WhenTheTableDeclaresNoCodePage() throws Exception {
    DbaseDecoder decoder = decoder(0x00, null, List.of(column("c", ColumnType.CHARACTER, 1, 0)),
        row(new byte[] {(byte) 0xe9}));
    assertEquals("é", decoder.next().values().get(0));
  }

  @Test
  void prefersTheCharsetItIsGivenOverTheCodePageOfTheTable() throws Exception {
    DbaseDecoder decoder = decoder(0x03, UTF_8, List.of(column("c", ColumnType.CHARACTER, 2, 0)),
        row("é".getBytes(UTF_8)));
    assertEquals("é", decoder.next().values().get(0));
  }

  @Test
  void readsNoMoreRecordsThanTheFileHolds() throws Exception {
    // A writer that interrupted itself leaves a count larger than the records it wrote.
    ByteBuffer buffer =
        table(0x03, null, List.of(column("c", ColumnType.CHARACTER, 3, 0)), row(text("aaa", 3)));
    buffer.order(ByteOrder.LITTLE_ENDIAN).putInt(4, 1000);

    DbaseDecoder decoder = new DbaseDecoder(buffer, null);
    assertEquals("aaa", decoder.next().values().get(0));
    assertNull(decoder.next());
  }

  @Test
  void rejectsColumnsThatAreNotTerminated() {
    ByteBuffer buffer =
        table(0x03, null, List.of(column("c", ColumnType.CHARACTER, 3, 0)), row(text("aaa", 3)));
    buffer.put(64, (byte) 'x');
    assertThrows(ShapefileException.class, () -> new DbaseDecoder(buffer, null));
  }

  @Test
  void rejectsAnUnknownColumnType() {
    ByteBuffer buffer =
        table(0x03, null, List.of(column("c", ColumnType.CHARACTER, 3, 0)), row(text("aaa", 3)));
    buffer.put(32 + 11, (byte) '!');
    assertThrows(ShapefileException.class, () -> new DbaseDecoder(buffer, null));
  }

  private static DbaseDecoder decoder(List<Column> columns, byte[]... records)
      throws ShapefileException {
    return decoder(0x03, null, columns, records);
  }

  private static DbaseDecoder decoder(int codePage, Charset charset, List<Column> columns,
      byte[]... records) throws ShapefileException {
    return new DbaseDecoder(table(codePage, charset, columns, records), charset);
  }

  private static Column column(String name, ColumnType type, int length, int decimals) {
    return new Column(name, type, length, decimals);
  }

  /** The bytes of a field of the given width, padded with spaces on the right. */
  private static byte[] text(String value, int length) {
    byte[] field = new byte[length];
    byte[] bytes = value.getBytes(ISO_8859_1);
    System.arraycopy(bytes, 0, field, 0, bytes.length);
    for (int i = bytes.length; i < length; i++) {
      field[i] = ' ';
    }
    return field;
  }

  private static byte[] row(byte[]... fields) {
    return record(' ', fields);
  }

  private static byte[] deletedRow(byte[]... fields) {
    return record('*', fields);
  }

  private static byte[] record(char flag, byte[]... fields) {
    int length = 1;
    for (byte[] field : fields) {
      length += field.length;
    }
    ByteBuffer record = ByteBuffer.allocate(length);
    record.put((byte) flag);
    for (byte[] field : fields) {
      record.put(field);
    }
    return record.array();
  }

  /** The bytes of a {@code .dbf} table holding the given columns and records. */
  private static ByteBuffer table(int codePage, Charset charset, List<Column> columns,
      byte[]... records) {
    int firstRecord = 32 + 32 * columns.size() + 1;
    int recordLength = 1;
    for (Column column : columns) {
      recordLength += column.length();
    }

    ByteBuffer buffer = ByteBuffer.allocate(firstRecord + records.length * recordLength)
        .order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(0, (byte) 0x03);
    buffer.putInt(4, records.length);
    buffer.putShort(8, (short) firstRecord);
    buffer.putShort(10, (short) recordLength);
    buffer.put(29, (byte) codePage);

    int offset = 32;
    for (Column column : columns) {
      buffer.put(offset, column.name().getBytes(US_ASCII));
      buffer.put(offset + 11, (byte) column.type().code());
      buffer.put(offset + 16, (byte) column.length());
      buffer.put(offset + 17, (byte) column.decimalCount());
      offset += 32;
    }
    buffer.put(offset, (byte) 0x0d);

    buffer.position(firstRecord);
    for (byte[] record : records) {
      buffer.put(record);
    }
    return buffer;
  }
}
