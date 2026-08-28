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

import java.util.Objects;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.type.SqlTypeName;

/**
 * A fixed column in a table with a simple type.
 */
public record DataColumnFixed(
    String name,
    DataColumn.Cardinality cardinality,
    RelDataType relDataType) implements DataColumn {

  /**
   * Constructs a fixed column with validation.
   *
   * @param name the name of the column
   * @param cardinality the cardinality of the column
   * @param relDataType the RelDataType of the column
   * @throws NullPointerException if any parameter is null
   */
  public DataColumnFixed {
    Objects.requireNonNull(name, "Column name cannot be null");
    Objects.requireNonNull(cardinality, "Column cardinality cannot be null");
    Objects.requireNonNull(relDataType, "Column type cannot be null");

    if (name.isBlank()) {
      throw new IllegalArgumentException("Column name cannot be blank");
    }
  }

  @Override
  public SqlTypeName sqlTypeName() {
    return relDataType.getSqlTypeName();
  }

  @Override
  public Class<?> javaType() {
    return sqlTypeNameToJavaClass(sqlTypeName());
  }

  /**
   * Converts a SqlTypeName to a Java class.
   *
   * @param sqlTypeName the SQL type name
   * @return the corresponding Java class
   */
  private static Class<?> sqlTypeNameToJavaClass(SqlTypeName sqlTypeName) {
    // Maps SqlTypeName to Java class
    switch (sqlTypeName) {
      case BOOLEAN:
        return Boolean.class;
      case TINYINT:
        return Byte.class;
      case SMALLINT:
        return Short.class;
      case INTEGER:
        return Integer.class;
      case BIGINT:
        return Long.class;
      case FLOAT:
      case REAL:
        return Float.class;
      case DOUBLE:
      case DECIMAL:
        return Double.class;
      case CHAR:
      case VARCHAR:
        return String.class;
      case BINARY:
      case VARBINARY:
        return byte[].class;
      case DATE:
        return java.time.LocalDate.class;
      case TIME:
        return java.time.LocalTime.class;
      case TIMESTAMP:
        return java.time.LocalDateTime.class;
      case GEOMETRY:
        return org.locationtech.jts.geom.Geometry.class;
      case ARRAY:
        return Object[].class;
      case MAP:
      case ROW:
      case STRUCTURED:
        return java.util.Map.class;
      default:
        return Object.class;
    }
  }
}
