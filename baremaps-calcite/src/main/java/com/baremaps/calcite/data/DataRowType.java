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

package com.baremaps.calcite.data;

import com.baremaps.data.type.BooleanDataType;
import com.baremaps.data.type.ByteArrayDataType;
import com.baremaps.data.type.ByteDataType;
import com.baremaps.data.type.DataType;
import com.baremaps.data.type.DoubleDataType;
import com.baremaps.data.type.FloatDataType;
import com.baremaps.data.type.GeometryDataType;
import com.baremaps.data.type.IntegerDataType;
import com.baremaps.data.type.LongDataType;
import com.baremaps.data.type.MemoryAlignedDataType;
import com.baremaps.data.type.ShortDataType;
import com.baremaps.data.type.StringDataType;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.sql.type.SqlTypeName;

/**
 * Serializes Calcite rows ({@code Object[]}) into the binary format of the {@code baremaps-data}
 * collections.
 *
 * <p>
 * Layout: an {@code int} holding the total row size, then one value per column. Each value is a
 * null marker byte followed, when the marker is set, by the column's encoding. The marker lives
 * here rather than in {@code NullableDataType} because the latter cannot compute the size of a null
 * variable-length value from the buffer.
 */
public final class DataRowType implements DataType<Object[]> {

  private static final Map<SqlTypeName, DataType<?>> TYPES = new EnumMap<>(SqlTypeName.class);

  static {
    TYPES.put(SqlTypeName.BOOLEAN, new BooleanDataType());
    TYPES.put(SqlTypeName.TINYINT, new ByteDataType());
    TYPES.put(SqlTypeName.SMALLINT, new ShortDataType());
    TYPES.put(SqlTypeName.INTEGER, new IntegerDataType());
    TYPES.put(SqlTypeName.BIGINT, new LongDataType());
    TYPES.put(SqlTypeName.FLOAT, new FloatDataType());
    TYPES.put(SqlTypeName.REAL, new FloatDataType());
    TYPES.put(SqlTypeName.DOUBLE, new DoubleDataType());
    TYPES.put(SqlTypeName.DECIMAL, new DoubleDataType()); // precision is not preserved
    TYPES.put(SqlTypeName.CHAR, new StringDataType());
    TYPES.put(SqlTypeName.VARCHAR, new StringDataType());
    TYPES.put(SqlTypeName.BINARY, new ByteArrayDataType());
    TYPES.put(SqlTypeName.VARBINARY, new ByteArrayDataType());
    TYPES.put(SqlTypeName.DATE, new LocalDateDataType());
    TYPES.put(SqlTypeName.TIME, new LocalTimeDataType());
    TYPES.put(SqlTypeName.TIMESTAMP, new LocalDateTimeDataType());
    TYPES.put(SqlTypeName.GEOMETRY, new GeometryDataType());
  }

  private final List<DataType<Object>> columnTypes;

  /**
   * Creates a row type for the given Calcite row type.
   *
   * @throws IllegalArgumentException if a column has a type that cannot be persisted
   */
  @SuppressWarnings("unchecked")
  public DataRowType(RelDataType rowType) {
    this.columnTypes = new ArrayList<>(rowType.getFieldCount());
    for (RelDataTypeField field : rowType.getFieldList()) {
      DataType<?> type = TYPES.get(field.getType().getSqlTypeName());
      if (type == null) {
        throw new IllegalArgumentException(
            "Column " + field.getName() + " has unsupported type " + field.getType());
      }
      columnTypes.add((DataType<Object>) type);
    }
  }

  @Override
  public int size(Object[] row) {
    int size = Integer.BYTES;
    for (int i = 0; i < columnTypes.size(); i++) {
      size += Byte.BYTES;
      if (row[i] != null) {
        size += columnTypes.get(i).size(row[i]);
      }
    }
    return size;
  }

  @Override
  public int size(ByteBuffer buffer, int position) {
    return buffer.getInt(position);
  }

  @Override
  public void write(ByteBuffer buffer, int position, Object[] row) {
    int p = position + Integer.BYTES;
    for (int i = 0; i < columnTypes.size(); i++) {
      Object value = row[i];
      buffer.put(p++, (byte) (value == null ? 0 : 1));
      if (value != null) {
        DataType<Object> type = columnTypes.get(i);
        type.write(buffer, p, value);
        p += type.size(buffer, p);
      }
    }
    buffer.putInt(position, p - position);
  }

  @Override
  public Object[] read(ByteBuffer buffer, int position) {
    Object[] row = new Object[columnTypes.size()];
    int p = position + Integer.BYTES;
    for (int i = 0; i < columnTypes.size(); i++) {
      if (buffer.get(p++) != 0) {
        DataType<Object> type = columnTypes.get(i);
        row[i] = type.read(buffer, p);
        p += type.size(buffer, p);
      }
    }
    return row;
  }

  private static final class LocalDateDataType extends MemoryAlignedDataType<LocalDate> {

    LocalDateDataType() {
      super(Long.BYTES);
    }

    @Override
    public void write(ByteBuffer buffer, int position, LocalDate value) {
      buffer.putLong(position, value.toEpochDay());
    }

    @Override
    public LocalDate read(ByteBuffer buffer, int position) {
      return LocalDate.ofEpochDay(buffer.getLong(position));
    }
  }

  private static final class LocalTimeDataType extends MemoryAlignedDataType<LocalTime> {

    LocalTimeDataType() {
      super(Long.BYTES);
    }

    @Override
    public void write(ByteBuffer buffer, int position, LocalTime value) {
      buffer.putLong(position, value.toNanoOfDay());
    }

    @Override
    public LocalTime read(ByteBuffer buffer, int position) {
      return LocalTime.ofNanoOfDay(buffer.getLong(position));
    }
  }

  private static final class LocalDateTimeDataType
      extends MemoryAlignedDataType<LocalDateTime> {

    LocalDateTimeDataType() {
      super(2 * Long.BYTES);
    }

    @Override
    public void write(ByteBuffer buffer, int position, LocalDateTime value) {
      buffer.putLong(position, value.toLocalDate().toEpochDay());
      buffer.putLong(position + Long.BYTES, value.toLocalTime().toNanoOfDay());
    }

    @Override
    public LocalDateTime read(ByteBuffer buffer, int position) {
      return LocalDateTime.of(
          LocalDate.ofEpochDay(buffer.getLong(position)),
          LocalTime.ofNanoOfDay(buffer.getLong(position + Long.BYTES)));
    }
  }
}
