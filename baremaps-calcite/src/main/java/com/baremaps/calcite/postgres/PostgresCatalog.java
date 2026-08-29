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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;

/**
 * Reads table definitions from the PostgreSQL catalog. {@code pg_catalog} is queried directly
 * rather than through JDBC metadata because the latter does not report materialized views.
 */
final class PostgresCatalog {

  /** Ordinary and partitioned tables, views, materialized views and foreign tables. */
  private static final String TABLE_NAMES = """
      SELECT c.relname
      FROM pg_catalog.pg_class c
      JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
      WHERE n.nspname = ? AND c.relkind IN ('r', 'p', 'v', 'm', 'f')
      ORDER BY c.relname
      """;

  private static final String COLUMNS = """
      SELECT a.attname, pg_catalog.format_type(a.atttypid, a.atttypmod), NOT a.attnotnull
      FROM pg_catalog.pg_attribute a
      JOIN pg_catalog.pg_class c ON a.attrelid = c.oid
      JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
      WHERE n.nspname = ? AND c.relname = ? AND a.attnum > 0 AND NOT a.attisdropped
      ORDER BY a.attnum
      """;

  private PostgresCatalog() {}

  static List<String> tableNames(DataSource dataSource, String schema) throws SQLException {
    List<String> names = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(TABLE_NAMES)) {
      statement.setString(1, schema);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          names.add(resultSet.getString(1));
        }
      }
    }
    return names;
  }

  /** @throws SQLException if the table does not exist */
  static RelDataType rowType(DataSource dataSource, String schema, String table,
      RelDataTypeFactory typeFactory) throws SQLException {
    RelDataTypeFactory.Builder builder = typeFactory.builder();
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(COLUMNS)) {
      statement.setString(1, schema);
      statement.setString(2, table);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          RelDataType type =
              PostgresTypeConversion.toRelDataType(typeFactory, resultSet.getString(2));
          builder.add(resultSet.getString(1),
              typeFactory.createTypeWithNullability(type, resultSet.getBoolean(3)));
        }
      }
    }
    if (builder.getFieldCount() == 0) {
      throw new SQLException("Table not found: " + schema + "." + table);
    }
    return builder.build();
  }
}
