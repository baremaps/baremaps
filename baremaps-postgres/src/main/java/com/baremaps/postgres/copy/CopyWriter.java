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


import de.bytefish.pgbulkinsert.pgsql.handlers.*;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.postgresql.copy.PGCopyOutputStream;
import org.postgresql.core.Oid;

/**
 * A helper for writing in a {@code PGCopyOutputStream}.
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

  public static final StringValueHandler STRING_HANDLER =
      new StringValueHandler();

  public static final CollectionValueHandler<String, Collection<String>> STRING_COLLECTION_HANDLER =
      new CollectionValueHandler<>(Oid.TEXT, new StringValueHandler());

  public static final BooleanValueHandler BOOLEAN_HANDLER =
      new BooleanValueHandler();

  public static final CollectionValueHandler<Boolean, Collection<Boolean>> BOOLEAN_COLLECTION_HANDLER =
      new CollectionValueHandler<>(Oid.BOOL, new BooleanValueHandler());

  public static final ByteValueHandler<Number> BYTE_HANDLER =
      new ByteValueHandler<>();

  public static final ByteArrayValueHandler BYTE_ARRAY_HANDLER =
      new ByteArrayValueHandler();

  public static final ShortValueHandler<Number> SHORT_HANDLER =
      new ShortValueHandler<>();

  public static final CollectionValueHandler<Short, Collection<Short>> SHORT_COLLECTION_HANDLER =
      new CollectionValueHandler<>(Oid.INT2, new ShortValueHandler<>());

  public static final IntegerValueHandler<Number> INTEGER_HANDLER =
      new IntegerValueHandler<>();

  public static final CollectionValueHandler<Integer, Collection<Integer>> INTEGER_COLLECTION_HANDLER =
      new CollectionValueHandler<>(Oid.INT4, new IntegerValueHandler<>());

  public static final LongValueHandler<Number> LONG_HANDLER =
      new LongValueHandler<>();

  public static final CollectionValueHandler<Long, Collection<Long>> LONG_COLLECTION_HANDLER =
      new CollectionValueHandler<>(Oid.INT8, new LongValueHandler<>());

  public static final FloatValueHandler<Number> FLOAT_HANDLER =
      new FloatValueHandler<>();

  public static final CollectionValueHandler<Float, Collection<Float>> FLOAT_COLLECTION_HANDLER =
      new CollectionValueHandler<>(Oid.FLOAT4, new FloatValueHandler<>());

  public static final DoubleValueHandler<Number> DOUBLE_HANDLER =
      new DoubleValueHandler<>();

  public static final CollectionValueHandler<Double, Collection<Double>> DOUBLE_COLLECTION_HANDLER =
      new CollectionValueHandler<>(Oid.FLOAT8, new DoubleValueHandler<>());

  public static final LocalDateValueHandler LOCAL_DATE_HANDLER =
      new LocalDateValueHandler();

  public static final LocalDateTimeValueHandler LOCAL_DATE_TIME_HANDLER =
      new LocalDateTimeValueHandler();

  public static final Inet4AddressValueHandler INET_4_ADDRESS_HANDLER =
      new Inet4AddressValueHandler();

  public static final Inet6AddressValueHandler INET_6_ADDRESS_HANDLER =
      new Inet6AddressValueHandler();

  public static final HstoreValueHandler HSTORE_HANDLER =
      new HstoreValueHandler();

  public static final JsonbValueHandler JSONB_HANDLER =
      new JsonbValueHandler();

  public static final GeometryValueHandler GEOMETRY_HANDLER =
      new GeometryValueHandler();

  public static final EnvelopeValueHandler ENVELOPE_HANDLER =
      new EnvelopeValueHandler();

  private final DataOutputStream data;

  /**
   * Creates a new writer with the specified {@code PGCopyOutputStream}.
   *
   * @param data
   */
  public CopyWriter(PGCopyOutputStream data) {
    this.data = new DataOutputStream(new BufferedOutputStream(data, 65536));
  }

  /**
   * Writes the header of the query.
   *
   * @throws IOException
   */
  public void writeHeader() throws IOException {
    // 11 bytes required header
    data.writeBytes("PGCOPY\n\377\r\n\0");
    // 32 bit integer indicating no OID
    data.writeInt(0);
    // 32 bit header extension area length
    data.writeInt(0);
  }

  /**
   * Writes the number of columns affected by the query.
   *
   * @param columns
   * @throws IOException
   */
  public void startRow(int columns) throws IOException {
    data.writeShort(columns);
  }

  /**
   * Writes a null value.
   *
   * @throws IOException
   */
  public void writeNull() throws IOException {
    data.writeInt(-1);
  }

  /**
   * Writes a null value.
   *
   * @param handler the value handler
   * @param value the value
   */
  public <T> void write(BaseValueHandler<T> handler, T value) {
    handler.handle(data, value);
  }

  /**
   * Writes a string value.
   *
   * @param value the value
   */
  public void write(String value) {
    STRING_HANDLER.handle(data, value);
  }

  /**
   * Writes a list of string values.
   *
   * @param value the value
   */
  public void write(List<String> value) {
    STRING_COLLECTION_HANDLER.handle(data, value);
  }

  /**
   * Writes a boolean value.
   *
   * @param value the value
   */
  public void writeBoolean(Boolean value) {
    BOOLEAN_HANDLER.handle(data, value);
  }

  /**
   * Writes a list of boolean values.
   *
   * @param value the value
   */
  public void writeBooleanList(List<Boolean> value) {
    BOOLEAN_COLLECTION_HANDLER.handle(data, value);
  }

  /**
   * Writes a byte value.
   *
   * @param value the value
   */
  public void writeByte(Byte value) {
    BYTE_HANDLER.handle(data, value);
  }

  /**
   * Writes a byte array value.
   *
   * @param value the value
   */
  public void writeByteArray(byte[] value) {
    BYTE_ARRAY_HANDLER.handle(data, value);
  }

  /**
   * Writes a short value.
   *
   * @param value the value
   */
  public void writeShort(Short value) {
    SHORT_HANDLER.handle(data, value);
  }

  /**
   * Writes a list of short values.
   *
   * @param value the value
   */
  public void writeShortList(List<Short> value) {
    SHORT_COLLECTION_HANDLER.handle(data, value);
  }

  /**
   * Writes an integer value.
   *
   * @param value the value
   */
  public void writeInteger(Integer value) {
    INTEGER_HANDLER.handle(data, value);
  }

  /**
   * Writes a list of integer values.
   *
   * @param value the value
   */
  public void writeIntegerList(List<Integer> value) {
    INTEGER_COLLECTION_HANDLER.handle(data, value);
  }

  /**
   * Writes a long value.
   *
   * @param value the value
   */
  public void writeLong(Long value) {
    LONG_HANDLER.handle(data, value);
  }

  /**
   * Writes a list of long values.
   *
   * @param value the value
   */
  public void writeLongList(List<Long> value) {
    LONG_COLLECTION_HANDLER.handle(data, value);
  }

  /**
   * Writes a float value.
   *
   * @param value the value
   */
  public void writeFloat(Float value) {
    FLOAT_HANDLER.handle(data, value);
  }

  /**
   * Writes a list of float values.
   *
   * @param value the value
   */
  public void writeFloatList(List<Float> value) {
    FLOAT_COLLECTION_HANDLER.handle(data, value);
  }

  /**
   * Writes a double value.
   *
   * @param value the value
   */
  public void writeDouble(Double value) {
    DOUBLE_HANDLER.handle(data, value);
  }

  /**
   * Writes a list of double values.
   *
   * @param value the value
   */
  public void writeDoubleArray(List<Double> value) {
    DOUBLE_COLLECTION_HANDLER.handle(data, value);
  }

  /**
   * Writes a date value.
   *
   * @param value the value
   */
  public void writeLocalDate(LocalDate value) {
    LOCAL_DATE_HANDLER.handle(data, value);
  }

  /**
   * Writes a list of date values.
   *
   * @param value the value
   */
  public void writeLocalDateTime(LocalDateTime value) {
    LOCAL_DATE_TIME_HANDLER.handle(data, value);
  }

  /**
   * Writes an inet adress value.
   *
   * @param value the value
   */
  public void writeInet4Adress(Inet4Address value) {
    INET_4_ADDRESS_HANDLER.handle(data, value);
  }

  /**
   * Writes a list of inet adress values.
   *
   * @param value the value
   */
  public void writeInet6Adress(Inet6Address value) {
    INET_6_ADDRESS_HANDLER.handle(data, value);
  }

  /**
   * Writes a map value.
   *
   * @param value the value
   */
  public void writeHstore(Map<String, String> value) {
    HSTORE_HANDLER.handle(data, value);
  }

  /**
   * Writes a jsonb array
   *
   * @param value the value
   */
  public void writeJsonb(String value) {
    JSONB_HANDLER.handle(data, value);
  }

  /**
   * Writes a geometry value.
   *
   * @param value the value
   */
  public void writeGeometry(Geometry value) {
    GEOMETRY_HANDLER.handle(data, value);
  }

  /**
   * Writes an envelope value.
   *
   * @param value the value
   */
  public void writeEnvelope(Envelope value) {
    ENVELOPE_HANDLER.handle(data, value);
  }

  /**
   * Writes the end of the row.
   *
   * @throws IOException
   */
  @Override
  public void close() throws IOException {
    data.writeShort(-1);
    data.flush();
    data.close();
  }
}
