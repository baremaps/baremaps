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

package com.baremaps.tasks;

import com.baremaps.calcite.postgres.PostgresDdlExecutor;
import com.baremaps.calcite.postgres.PostgresSchema;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies the tables of a Calcite schema into PostgreSQL with {@code CREATE TABLE ... AS SELECT *},
 * executed by {@link PostgresDdlExecutor}.
 */
public final class PostgresImport {

  private static final Logger logger = LoggerFactory.getLogger(PostgresImport.class);

  private PostgresImport() {}

  /**
   * Copies tables into the {@code public} schema of {@code dataSource}.
   *
   * @param source the schema holding the tables to copy
   * @param tables the tables to copy, keyed by source name, with the target name as value
   * @param srid the SRID to assign to the geometry columns of the created tables
   * @return the number of rows copied per target table
   */
  public static Map<String, Long> copy(DataSource dataSource, Schema source,
      Map<String, String> tables, int srid) throws SQLException {
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");
    info.setProperty("caseSensitive", "false");
    info.setProperty("unquotedCasing", "TO_LOWER");
    info.setProperty("quotedCasing", "TO_LOWER");
    info.setProperty("parserFactory", PostgresDdlExecutor.class.getName() + "#PARSER_FACTORY");
    Map<String, Long> counts = new LinkedHashMap<>();
    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();
      rootSchema.add("source", source);
      rootSchema.add("pg",
          new PostgresSchema(dataSource, "public", calciteConnection.getTypeFactory()));
      calciteConnection.setSchema("pg");
      for (Map.Entry<String, String> table : tables.entrySet()) {
        String target = tableName(table.getValue());
        String sql = "CREATE TABLE " + target + " AS SELECT * FROM source." + table.getKey();
        logger.info("Executing: {}", sql);
        try (Statement statement = connection.createStatement()) {
          statement.execute(sql);
        }
        setSrid(dataSource, target, srid);
        counts.put(target, count(dataSource, target));
      }
    }
    return counts;
  }

  /**
   * Copies tables and logs how many rows each of them received.
   *
   * @see #copy(DataSource, Schema, Map, int)
   */
  public static void copyAndReport(DataSource dataSource, Schema source,
      Map<String, String> tables, int srid) throws SQLException {
    copy(dataSource, source, tables, srid)
        .forEach((table, count) -> logger.info("Imported {} rows to table: {}", count, table));
  }

  /** Returns a lower-case identifier safe to embed unquoted in SQL. */
  public static String tableName(String name) {
    return name.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase(Locale.ROOT);
  }

  private static void setSrid(DataSource dataSource, String table, int srid) throws SQLException {
    List<String> geometryColumns = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT f_geometry_column FROM geometry_columns WHERE f_table_name = ?")) {
      statement.setString(1, table);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          geometryColumns.add(resultSet.getString(1));
        }
      }
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("SELECT UpdateGeometrySRID(?, ?, ?)")) {
      for (String column : geometryColumns) {
        statement.setString(1, table);
        statement.setString(2, column);
        statement.setInt(3, srid);
        statement.execute();
      }
    }
  }

  private static long count(DataSource dataSource, String table) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM \"" + table + "\"")) {
      resultSet.next();
      return resultSet.getLong(1);
    }
  }
}
