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

import com.baremaps.calcite.DdlBackend;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.ViewTableMacro;

/**
 * DDL backend that creates objects in PostgreSQL.
 *
 * <p>
 * The target Calcite schema must wrap a {@link PostgresSchema}: that is where the
 * {@code DataSource} and the PostgreSQL schema name come from, so no global state is needed to
 * reach the database.
 */
final class PostgresDdlBackend implements DdlBackend {

  private static PostgresSchema postgres(CalciteSchema schema) {
    if (schema.schema instanceof PostgresSchema postgres) {
      return postgres;
    }
    throw new IllegalStateException("Schema " + schema.getName()
        + " is not a PostgreSQL schema; register a PostgresSchema and create the object in it");
  }

  private static String quote(PostgresSchema schema, String name) {
    return "\"" + schema.name() + "\".\"" + name + "\"";
  }

  private static void execute(PostgresSchema schema, String sql) {
    try (Connection connection = schema.dataSource().getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(sql);
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to execute: " + sql, e);
    }
  }

  @Override
  public void createTable(CalciteSchema schema, String name, RelDataType rowType,
      Map<String, String> options, RelDataTypeFactory typeFactory, Runnable populate) {
    PostgresSchema postgres = postgres(schema);
    String columns = rowType.getFieldList().stream()
        .map(PostgresDdlBackend::columnDefinition)
        .collect(Collectors.joining(", "));
    execute(postgres, "CREATE TABLE " + quote(postgres, name) + " (" + columns + ")");
    schema.add(name, table(postgres, name, typeFactory));
    populate.run();
  }

  private static String columnDefinition(RelDataTypeField field) {
    String definition = "\"" + field.getName() + "\" "
        + PostgresTypeConversion.toPostgresTypeString(field.getType());
    return field.getType().isNullable() ? definition : definition + " NOT NULL";
  }

  private static Table table(PostgresSchema postgres, String name,
      RelDataTypeFactory typeFactory) {
    try {
      return new PostgresModifiableTable(postgres.dataSource(), postgres.name(), name,
          typeFactory);
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to read the definition of " + name, e);
    }
  }

  @Override
  public void dropTable(CalciteSchema schema, String name) {
    PostgresSchema postgres = postgres(schema);
    Table table = schema.plus().getTable(name);
    schema.removeTable(name);
    String kind = table != null && table.getJdbcTableType() == Schema.TableType.MATERIALIZED_VIEW
        ? "MATERIALIZED VIEW"
        : "TABLE";
    execute(postgres, "DROP " + kind + " IF EXISTS " + quote(postgres, name));
  }

  @Override
  public void createMaterializedView(CalciteSchema schema, String name, RelDataType rowType,
      String sql, RelDataTypeFactory typeFactory, Runnable populate) {
    PostgresSchema postgres = postgres(schema);
    // The view query is handed to PostgreSQL as is, so it may only reference PostgreSQL objects.
    execute(postgres, "CREATE MATERIALIZED VIEW " + quote(postgres, name) + " AS " + sql);
    try {
      schema.add(name, new PostgresMaterializedView(postgres.dataSource(), postgres.name(), name,
          typeFactory));
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to read the definition of " + name, e);
    }
  }

  @Override
  public void createView(CalciteSchema schema, String name, String sql, boolean replace,
      ViewTableMacro view) {
    PostgresSchema postgres = postgres(schema);
    String orReplace = replace ? "OR REPLACE " : "";
    execute(postgres, "CREATE " + orReplace + "VIEW " + quote(postgres, name) + " AS " + sql);
    schema.plus().add(name, view);
  }

  @Override
  public void dropView(CalciteSchema schema, String name) {
    PostgresSchema postgres = postgres(schema);
    schema.removeFunction(name);
    execute(postgres, "DROP VIEW IF EXISTS " + quote(postgres, name));
  }

  @Override
  public void createSchema(CalciteSchema parent, String name) {
    // A PostgresSchema is registered per PostgreSQL schema by the application; creating one from
    // SQL would need a DataSource that the parent (usually the root) schema does not carry.
    throw new UnsupportedOperationException(
        "CREATE SCHEMA is not supported for PostgreSQL; register a PostgresSchema instead");
  }

  @Override
  public void dropSchema(CalciteSchema parent, String name) {
    throw new UnsupportedOperationException("DROP SCHEMA is not supported for PostgreSQL");
  }
}
