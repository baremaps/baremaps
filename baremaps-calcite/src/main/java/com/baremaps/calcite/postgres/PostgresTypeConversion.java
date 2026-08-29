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

package com.baremaps.calcite.postgres;

import java.util.Locale;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;

/** Maps between PostgreSQL type names and Calcite types. */
final class PostgresTypeConversion {

  private PostgresTypeConversion() {}

  /**
   * Converts a PostgreSQL type name, as written in DDL or reported by {@code format_type}, into a
   * Calcite type. Type modifiers such as {@code (Point, 4326)} are ignored; unknown types become
   * {@code VARCHAR} so that any column can at least be read as text.
   */
  static RelDataType toRelDataType(RelDataTypeFactory typeFactory, String postgresType) {
    String name = postgresType.toLowerCase(Locale.ROOT);
    int modifier = name.indexOf('(');
    if (modifier >= 0) {
      name = name.substring(0, modifier).trim();
    }
    SqlTypeName sqlTypeName = switch (name) {
      case "int4", "integer", "int", "serial" -> SqlTypeName.INTEGER;
      case "int8", "bigint", "bigserial" -> SqlTypeName.BIGINT;
      case "int2", "smallint" -> SqlTypeName.SMALLINT;
      case "float4", "real" -> SqlTypeName.FLOAT;
      case "float8", "double precision" -> SqlTypeName.DOUBLE;
      case "numeric", "decimal" -> SqlTypeName.DECIMAL;
      case "bool", "boolean" -> SqlTypeName.BOOLEAN;
      case "varchar", "character varying", "text" -> SqlTypeName.VARCHAR;
      case "char", "character", "bpchar" -> SqlTypeName.CHAR;
      case "date" -> SqlTypeName.DATE;
      case "timestamp", "timestamp without time zone" -> SqlTypeName.TIMESTAMP;
      case "timestamptz", "timestamp with time zone" -> SqlTypeName.TIMESTAMP_WITH_LOCAL_TIME_ZONE;
      case "time", "time without time zone" -> SqlTypeName.TIME;
      case "timetz", "time with time zone" -> SqlTypeName.TIME_WITH_LOCAL_TIME_ZONE;
      case "bytea" -> SqlTypeName.BINARY;
      case "geometry", "geography" -> SqlTypeName.GEOMETRY;
      case "json", "jsonb" -> SqlTypeName.OTHER;
      default -> SqlTypeName.VARCHAR;
    };
    return typeFactory.createSqlType(sqlTypeName);
  }

  /** Converts a Calcite type into the PostgreSQL type to declare in {@code CREATE TABLE}. */
  static String toPostgresTypeString(RelDataType type) {
    if (type.isStruct()) {
      return "JSONB";
    }
    return switch (type.getSqlTypeName()) {
      case INTEGER -> "INTEGER";
      case BIGINT -> "BIGINT";
      case SMALLINT, TINYINT -> "SMALLINT";
      case FLOAT, REAL -> "REAL";
      case DOUBLE, DECIMAL -> "DOUBLE PRECISION";
      case BOOLEAN -> "BOOLEAN";
      case VARCHAR, CHAR -> {
        int precision = type.getPrecision();
        if (precision == RelDataType.PRECISION_NOT_SPECIFIED) {
          yield "TEXT";
        }
        yield (type.getSqlTypeName() == SqlTypeName.VARCHAR ? "VARCHAR(" : "CHAR(")
            + precision + ")";
      }
      case DATE -> "DATE";
      case TIMESTAMP -> "TIMESTAMP";
      case TIMESTAMP_WITH_LOCAL_TIME_ZONE -> "TIMESTAMP WITH TIME ZONE";
      case TIME -> "TIME";
      case TIME_WITH_LOCAL_TIME_ZONE -> "TIME WITH TIME ZONE";
      case BINARY, VARBINARY -> "BYTEA";
      case GEOMETRY -> "GEOMETRY";
      case ARRAY -> toPostgresTypeString(type.getComponentType()) + "[]";
      case OTHER -> "JSONB";
      default -> "TEXT";
    };
  }
}
