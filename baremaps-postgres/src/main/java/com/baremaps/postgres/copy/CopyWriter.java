/*
 * The MIT License (MIT)
 *
 * Copyright (c) The PgBulkInsert Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.baremaps.postgres.copy;

import de.bytefish.pgbulkinsert.pgsql.handlers.BooleanValueHandler;
import de.bytefish.pgbulkinsert.pgsql.handlers.ByteValueHandler;
import de.bytefish.pgbulkinsert.pgsql.handlers.CollectionValueHandler;
import de.bytefish.pgbulkinsert.pgsql.handlers.DoubleValueHandler;
import de.bytefish.pgbulkinsert.pgsql.handlers.FloatValueHandler;
import de.bytefish.pgbulkinsert.pgsql.handlers.IntegerValueHandler;
import de.bytefish.pgbulkinsert.pgsql.handlers.LocalDateTimeValueHandler;
import de.bytefish.pgbulkinsert.pgsql.handlers.LocalDateValueHandler;
import de.bytefish.pgbulkinsert.pgsql.handlers.LongValueHandler;
import de.bytefish.pgbulkinsert.pgsql.handlers.ShortValueHandler;
import de.bytefish.pgbulkinsert.pgsql.handlers.StringValueHandler;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.locationtech.jts.geom.Geometry;
import org.postgresql.copy.PGCopyOutputStream;
import org.postgresql.core.Oid;

/**
 * Writes rows in the binary format of the Postgres {@code COPY} command.
 *
 * <p>
 * A caller writes the header once, then one {@link #startRow} and one {@code write} per column for
 * each row, in the order the {@code COPY} statement named the columns. Passing {@code null} to any
 * of the {@code write} methods writes an SQL null.
 *
 * <p>
 * The value handlers that encode each type are an implementation detail and stay private: the module
 * writes a fixed set of Postgres types, and a method per type is what keeps the column order at a
 * call site readable against the statement it mirrors.
 *
 * <p>
 * This code has been adapted from
 * <a href="https://github.com/PgBulkInsert/PgBulkInsert">PgBulkInsert</a> licensed under the MIT
 * license.
 *
 * <p>
 * Copyright (c) The PgBulkInsert Team.
 */
public class CopyWriter implements AutoCloseable {

  private static final StringValueHandler STRING_HANDLER = new StringValueHandler();

  private static final CollectionValueHandler<String, Collection<String>> STRING_COLLECTION_HANDLER =
      new CollectionValueHandler<>(Oid.TEXT, new StringValueHandler());

  private static final BooleanValueHandler BOOLEAN_HANDLER = new BooleanValueHandler();

  private static final ByteValueHandler<Number> BYTE_HANDLER = new ByteValueHandler<>();

  private static final ShortValueHandler<Number> SHORT_HANDLER = new ShortValueHandler<>();

  private static final IntegerValueHandler<Number> INTEGER_HANDLER = new IntegerValueHandler<>();

  private static final CollectionValueHandler<Integer, Collection<Integer>> INTEGER_COLLECTION_HANDLER =
      new CollectionValueHandler<>(Oid.INT4, new IntegerValueHandler<>());

  private static final LongValueHandler<Number> LONG_HANDLER = new LongValueHandler<>();

  private static final CollectionValueHandler<Long, Collection<Long>> LONG_COLLECTION_HANDLER =
      new CollectionValueHandler<>(Oid.INT8, new LongValueHandler<>());

  private static final FloatValueHandler<Number> FLOAT_HANDLER = new FloatValueHandler<>();

  private static final DoubleValueHandler<Number> DOUBLE_HANDLER = new DoubleValueHandler<>();

  private static final LocalDateValueHandler LOCAL_DATE_HANDLER = new LocalDateValueHandler();

  private static final LocalDateTimeValueHandler LOCAL_DATE_TIME_HANDLER =
      new LocalDateTimeValueHandler();

  private static final JsonbValueHandler JSONB_HANDLER = new JsonbValueHandler();

  private static final GeometryValueHandler GEOMETRY_HANDLER = new GeometryValueHandler();

  /**
   * The copy stream sends whatever it is handed, so the rows are buffered into batches large enough
   * to keep the round trips down during an import of hundreds of millions of entities.
   */
  private static final int BUFFER_SIZE = 65536;

  private final DataOutputStream data;

  /**
   * Creates a new writer over the given copy stream.
   *
   * @param data the stream opened for the {@code COPY ... FROM STDIN BINARY} statement
   */
  public CopyWriter(PGCopyOutputStream data) {
    this.data = new DataOutputStream(new BufferedOutputStream(data, BUFFER_SIZE));
  }

  /** Writes the file header, once, before the first row. */
  public void writeHeader() throws IOException {
    // 11 bytes required header
    data.writeBytes("PGCOPY\n\377\r\n\0");
    // 32 bit integer indicating no OID
    data.writeInt(0);
    // 32 bit header extension area length
    data.writeInt(0);
  }

  /**
   * Starts a row.
   *
   * @param columns the number of columns the row is about to write
   */
  public void startRow(int columns) throws IOException {
    data.writeShort(columns);
  }

  /** Writes a null value. */
  public void writeNull() throws IOException {
    data.writeInt(-1);
  }

  /** Writes a text value. */
  public void write(String value) {
    STRING_HANDLER.handle(data, value);
  }

  /** Writes an array of text values. */
  public void write(List<String> value) {
    STRING_COLLECTION_HANDLER.handle(data, value);
  }

  /** Writes a boolean value. */
  public void writeBoolean(Boolean value) {
    BOOLEAN_HANDLER.handle(data, value);
  }

  /** Writes a byte value. */
  public void writeByte(Byte value) {
    BYTE_HANDLER.handle(data, value);
  }

  /** Writes a short value. */
  public void writeShort(Short value) {
    SHORT_HANDLER.handle(data, value);
  }

  /** Writes an integer value. */
  public void writeInteger(Integer value) {
    INTEGER_HANDLER.handle(data, value);
  }

  /** Writes an array of integer values. */
  public void writeIntegerList(List<Integer> value) {
    INTEGER_COLLECTION_HANDLER.handle(data, value);
  }

  /** Writes a long value. */
  public void writeLong(Long value) {
    LONG_HANDLER.handle(data, value);
  }

  /** Writes an array of long values. */
  public void writeLongList(List<Long> value) {
    LONG_COLLECTION_HANDLER.handle(data, value);
  }

  /** Writes a float value. */
  public void writeFloat(Float value) {
    FLOAT_HANDLER.handle(data, value);
  }

  /** Writes a double value. */
  public void writeDouble(Double value) {
    DOUBLE_HANDLER.handle(data, value);
  }

  /** Writes a date value. */
  public void writeLocalDate(LocalDate value) {
    LOCAL_DATE_HANDLER.handle(data, value);
  }

  /** Writes a timestamp value. */
  public void writeLocalDateTime(LocalDateTime value) {
    LOCAL_DATE_TIME_HANDLER.handle(data, value);
  }

  /**
   * Writes a jsonb value.
   *
   * @param value the value, already serialized as JSON; see {@link JsonbValueHandler}
   */
  public void writeJsonb(String value) {
    JSONB_HANDLER.handle(data, value);
  }

  /** Writes a geometry value, as EWKB. */
  public void writeGeometry(Geometry value) {
    GEOMETRY_HANDLER.handle(data, value);
  }

  /**
   * Writes the file trailer and closes the stream, ending the copy.
   *
   * <p>
   * The trailer is written unconditionally, so a caller that abandons a copy part way through a
   * try-with-resources still ends it, and the rows already flushed are committed. Callers of this
   * class delete the rows they are about to copy first, which makes replaying the batch after such a
   * failure produce the same table.
   */
  @Override
  public void close() throws IOException {
    try (data) {
      data.writeShort(-1);
    }
  }
}
