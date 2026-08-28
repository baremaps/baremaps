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

import com.baremaps.data.type.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.calcite.sql.type.SqlTypeName;

/**
 * A {@link DataType} for reading and writing {@link DataRow} objects in {@link ByteBuffer}s.
 */
public class DataRowType implements DataType<DataRow> {

  /**
   * Registry of data types for each of the supported SQL type names.
   */
  private static final Map<SqlTypeName, DataType<?>> TYPE_REGISTRY = new ConcurrentHashMap<>();

  // Initialize standard type mappings
  static {
    registerStandardTypes();
  }

  /**
   * Registers standard data types.
   */
  private static void registerStandardTypes() {
    registerType(SqlTypeName.BOOLEAN, new BooleanDataType());
    registerType(SqlTypeName.TINYINT, new ByteDataType());
    registerType(SqlTypeName.SMALLINT, new ShortDataType());
    registerType(SqlTypeName.INTEGER, new IntegerDataType());
    registerType(SqlTypeName.BIGINT, new LongDataType());
    registerType(SqlTypeName.FLOAT, new FloatDataType());
    registerType(SqlTypeName.REAL, new FloatDataType());
    registerType(SqlTypeName.DOUBLE, new DoubleDataType());
    registerType(SqlTypeName.DECIMAL, new DoubleDataType()); // Simplified mapping
    registerType(SqlTypeName.CHAR, new StringDataType());
    registerType(SqlTypeName.VARCHAR, new StringDataType());
    registerType(SqlTypeName.BINARY, new BinaryDataType());
    registerType(SqlTypeName.VARBINARY, new BinaryDataType());
    registerType(SqlTypeName.DATE, new LocalDateDataType());
    registerType(SqlTypeName.TIME, new LocalTimeDataType());
    registerType(SqlTypeName.TIMESTAMP, new LocalDateTimeDataType());

    // Geometry Types
    registerType(SqlTypeName.GEOMETRY, new GeometryDataType());

    // Custom mappings for JTS geometry types
    // These would ideally be based on Calcite types, but we'll handle them specially for now
    registerGeometrySubtypes();
  }

  /**
   * Registers geometry subtypes.
   */
  private static void registerGeometrySubtypes() {
    // Custom handler for different geometry types
    // In a full implementation, we might extend SqlTypeName or use attributes
    // For now, we'll handle these as special cases
    TYPE_REGISTRY.put(SqlTypeName.OTHER, new PointDataType()); // Special case for POINT
    TYPE_REGISTRY.put(SqlTypeName.DISTINCT, new LineStringDataType()); // Special case for
                                                                       // LINESTRING
    TYPE_REGISTRY.put(SqlTypeName.STRUCTURED, new PolygonDataType()); // Special case for POLYGON
    // We're using some SqlTypeNames that aren't perfect matches but work for the registry
    // In a real implementation, we might use a more sophisticated approach
  }

  /**
   * Registers a custom data type for a SQL type name.
   *
   * @param sqlTypeName the SQL type name
   * @param dataType the data type
   * @param <T> the data type's value type
   * @throws IllegalArgumentException if a type is already registered for the SQL type name
   */
  public static <T> void registerType(SqlTypeName sqlTypeName, DataType<T> dataType) {
    Objects.requireNonNull(sqlTypeName, "SQL type name cannot be null");
    Objects.requireNonNull(dataType, "Data type cannot be null");

    TYPE_REGISTRY.put(sqlTypeName, dataType);
  }

  /**
   * Gets the data type for a SQL type name.
   *
   * @param sqlTypeName the SQL type name
   * @return the data type
   * @throws IllegalArgumentException if no type is registered for the SQL type name
   */
  @SuppressWarnings("unchecked")
  public static <T> DataType<T> getType(SqlTypeName sqlTypeName) {
    Objects.requireNonNull(sqlTypeName, "SQL type name cannot be null");

    DataType<T> dataType = (DataType<T>) TYPE_REGISTRY.get(sqlTypeName);
    if (dataType == null) {
      throw new IllegalArgumentException(
          "No data type registered for SQL type name: " + sqlTypeName);
    }
    return dataType;
  }

  private final DataTableSchema rowType;

  /**
   * Constructs a DataRowType with the given schema.
   *
   * @param rowType the row schema
   */
  public DataRowType(DataTableSchema rowType) {
    this.rowType = Objects.requireNonNull(rowType, "Row type cannot be null");
  }

  /** {@inheritDoc} */
  @Override
  public int size(final DataRow row) {
    Objects.requireNonNull(row, "Row cannot be null");

    int size = Integer.BYTES;
    var columns = rowType.columns();
    for (int i = 0; i < columns.size(); i++) {
      var column = columns.get(i);
      try {
        var dataType = getType(column.sqlTypeName());
        var value = row.get(i);
        size += dataType.size(value);
      } catch (IllegalArgumentException e) {
        throw new IllegalStateException("Error determining size for column " + i +
            " with type " + column.sqlTypeName(), e);
      }
    }
    return size;
  }

  /** {@inheritDoc} */
  @Override
  public int size(final ByteBuffer buffer, final int position) {
    Objects.requireNonNull(buffer, "Buffer cannot be null");
    if (position < 0 || position >= buffer.capacity()) {
      throw new IllegalArgumentException("Invalid position: " + position);
    }

    return buffer.getInt(position);
  }

  /** {@inheritDoc} */
  @Override
  public void write(final ByteBuffer buffer, final int position, final DataRow row) {
    Objects.requireNonNull(buffer, "Buffer cannot be null");
    Objects.requireNonNull(row, "Row cannot be null");
    if (position < 0 || position >= buffer.capacity()) {
      throw new IllegalArgumentException("Invalid position: " + position);
    }

    int p = position + Integer.BYTES;
    var columns = rowType.columns();
    for (int i = 0; i < columns.size(); i++) {
      var column = columns.get(i);
      try {
        var dataType = getType(column.sqlTypeName());
        var value = row.get(i);
        dataType.write(buffer, p, value);
        p += dataType.size(buffer, p);
      } catch (IllegalArgumentException e) {
        throw new IllegalStateException("Error writing column " + i +
            " with type " + column.sqlTypeName(), e);
      }
    }
    buffer.putInt(position, p - position);
  }

  /** {@inheritDoc} */
  @Override
  public DataRow read(final ByteBuffer buffer, final int position) {
    Objects.requireNonNull(buffer, "Buffer cannot be null");
    if (position < 0 || position >= buffer.capacity()) {
      throw new IllegalArgumentException("Invalid position: " + position);
    }

    int p = position + Integer.BYTES;
    var columns = rowType.columns();
    var values = new ArrayList<>(columns.size());
    for (int i = 0; i < columns.size(); i++) {
      var column = columns.get(i);
      try {
        var dataType = getType(column.sqlTypeName());
        values.add(dataType.read(buffer, p));
        p += dataType.size(buffer, p);
      } catch (IllegalArgumentException e) {
        throw new IllegalStateException("Error reading column " + i +
            " with type " + column.sqlTypeName(), e);
      }
    }
    return new DataRow(rowType, values);
  }

  /**
   * Class for handling binary data.
   */
  private static class BinaryDataType implements DataType<byte[]> {
    @Override
    public int size(byte[] value) {
      return Integer.BYTES + (value != null ? value.length : 0);
    }

    @Override
    public int size(ByteBuffer buffer, int position) {
      int length = buffer.getInt(position);
      return Integer.BYTES + (length >= 0 ? length : 0);
    }

    @Override
    public void write(ByteBuffer buffer, int position, byte[] value) {
      if (value == null) {
        buffer.putInt(position, -1);
      } else {
        buffer.putInt(position, value.length);
        for (int i = 0; i < value.length; i++) {
          buffer.put(position + Integer.BYTES + i, value[i]);
        }
      }
    }

    @Override
    public byte[] read(ByteBuffer buffer, int position) {
      int length = buffer.getInt(position);
      if (length < 0) {
        return null;
      }
      byte[] value = new byte[length];
      for (int i = 0; i < length; i++) {
        value[i] = buffer.get(position + Integer.BYTES + i);
      }
      return value;
    }
  }

  /**
   * Class for handling LocalDate data.
   */
  private static class LocalDateDataType implements DataType<java.time.LocalDate> {
    @Override
    public int size(java.time.LocalDate value) {
      return value == null ? Integer.BYTES : (Integer.BYTES + Long.BYTES);
    }

    @Override
    public int size(ByteBuffer buffer, int position) {
      return buffer.getInt(position) < 0 ? Integer.BYTES : (Integer.BYTES + Long.BYTES);
    }

    @Override
    public void write(ByteBuffer buffer, int position, java.time.LocalDate value) {
      if (value == null) {
        buffer.putInt(position, -1);
      } else {
        buffer.putInt(position, 0); // Not null
        buffer.putLong(position + Integer.BYTES, value.toEpochDay());
      }
    }

    @Override
    public java.time.LocalDate read(ByteBuffer buffer, int position) {
      if (buffer.getInt(position) < 0) {
        return null;
      }
      return java.time.LocalDate.ofEpochDay(buffer.getLong(position + Integer.BYTES));
    }
  }

  /**
   * Class for handling LocalTime data.
   */
  private static class LocalTimeDataType implements DataType<java.time.LocalTime> {
    @Override
    public int size(java.time.LocalTime value) {
      return value == null ? Integer.BYTES : (Integer.BYTES + Long.BYTES);
    }

    @Override
    public int size(ByteBuffer buffer, int position) {
      return buffer.getInt(position) < 0 ? Integer.BYTES : (Integer.BYTES + Long.BYTES);
    }

    @Override
    public void write(ByteBuffer buffer, int position, java.time.LocalTime value) {
      if (value == null) {
        buffer.putInt(position, -1);
      } else {
        buffer.putInt(position, 0); // Not null
        buffer.putLong(position + Integer.BYTES, value.toNanoOfDay());
      }
    }

    @Override
    public java.time.LocalTime read(ByteBuffer buffer, int position) {
      if (buffer.getInt(position) < 0) {
        return null;
      }
      return java.time.LocalTime.ofNanoOfDay(buffer.getLong(position + Integer.BYTES));
    }
  }

  /**
   * Class for handling LocalDateTime data.
   */
  private static class LocalDateTimeDataType implements DataType<java.time.LocalDateTime> {
    @Override
    public int size(java.time.LocalDateTime value) {
      return value == null ? Integer.BYTES : (Integer.BYTES + 2 * Long.BYTES);
    }

    @Override
    public int size(ByteBuffer buffer, int position) {
      return buffer.getInt(position) < 0 ? Integer.BYTES : (Integer.BYTES + 2 * Long.BYTES);
    }

    @Override
    public void write(ByteBuffer buffer, int position, java.time.LocalDateTime value) {
      if (value == null) {
        buffer.putInt(position, -1);
      } else {
        buffer.putInt(position, 0); // Not null
        buffer.putLong(position + Integer.BYTES, value.toLocalDate().toEpochDay());
        buffer.putLong(position + Integer.BYTES + Long.BYTES, value.toLocalTime().toNanoOfDay());
      }
    }

    @Override
    public java.time.LocalDateTime read(ByteBuffer buffer, int position) {
      if (buffer.getInt(position) < 0) {
        return null;
      }
      java.time.LocalDate date = java.time.LocalDate.ofEpochDay(
          buffer.getLong(position + Integer.BYTES));
      java.time.LocalTime time = java.time.LocalTime.ofNanoOfDay(
          buffer.getLong(position + Integer.BYTES + Long.BYTES));
      return java.time.LocalDateTime.of(date, time);
    }
  }
}
