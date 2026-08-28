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

package com.baremaps.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.calcite.geoparquet.GeoParquetSchema;
import com.baremaps.calcite.postgres.PostgresDdlExecutor;
import com.baremaps.testing.PostgresContainerTest;
import com.baremaps.testing.TestFiles;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class GeoParquetToPostgresTest extends PostgresContainerTest {

  @Test
  @Tag("integration")
  void copyGeoParquetToPostgres() throws Exception {
    // Open the GeoParquet
    var uri = TestFiles.GEOPARQUET.toUri();

    // Set ThreadLocal DataSource for PostgresDdlExecutor to use
    PostgresDdlExecutor.setThreadLocalDataSource(dataSource());

    try {
      // Setup Calcite connection properties
      Properties info = new Properties();
      info.setProperty("lex", "MYSQL");
      info.setProperty("caseSensitive", "false");
      info.setProperty("unquotedCasing", "TO_LOWER");
      info.setProperty("quotedCasing", "TO_LOWER");
      info.setProperty("parserFactory", PostgresDdlExecutor.class.getName() + "#PARSER_FACTORY");

      // Create a connection to Calcite
      try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
        CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
        SchemaPlus rootSchema = calciteConnection.getRootSchema();

        // Register the GeoParquet schema
        Schema geoParquetSchema = new GeoParquetSchema(uri);
        rootSchema.add("geoparquet", geoParquetSchema);

        // Get the list of tables in the GeoParquet
        String[] tables = getGeoParquetTables(connection);

        assertTrue(tables.length > 0, "No tables found in GeoParquet");

        // Import each table
        for (String tableName : tables) {
          // Register the GeoParquet table in the Calcite schema
          String registerSql = "CREATE TABLE " + tableName + " AS " +
              "SELECT * FROM geoparquet." + tableName;

          // Execute the DDL statement to create the table
          try (Statement statement = connection.createStatement()) {
            statement.execute(registerSql);
          }

          // Verify that the table was created in PostgreSQL
          try (Connection pgConnection = dataSource().getConnection();
              Statement statement = pgConnection.createStatement();
              ResultSet resultSet = statement.executeQuery(
                  "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = '" +
                      tableName + "')")) {
            assertTrue(resultSet.next() && resultSet.getBoolean(1),
                "Failed to create table: " + tableName);
          }

          // Verify that the table has data
          try (Connection pgConnection = dataSource().getConnection();
              Statement statement = pgConnection.createStatement();
              ResultSet resultSet = statement.executeQuery(
                  "SELECT COUNT(*) FROM " + tableName)) {
            assertTrue(resultSet.next(), "No rows found in table: " + tableName);
            int count = resultSet.getInt(1);
            assertTrue(count > 0, "Expected rows in table: " + tableName);
          }
        }
      }
    } finally {
      // Clean up thread local storage
      PostgresDdlExecutor.clearThreadLocalDataSource();
    }
  }

  /**
   * Gets the list of tables in the GeoParquet.
   * 
   * @param connection the Calcite connection
   * @return the list of table names
   * @throws Exception if an error occurs
   */
  private String[] getGeoParquetTables(Connection connection) throws Exception {
    try (Statement statement = connection.createStatement()) {
      DatabaseMetaData metaData = connection.getMetaData();
      ResultSet resultSet = metaData.getTables("geoparquet", null, null, new String[] {"TABLE"});
      java.util.List<String> tables = new java.util.ArrayList<>();
      while (resultSet.next()) {
        tables.add(resultSet.getString("TABLE_NAME"));
      }
      return tables.toArray(new String[0]);
    }
  }
}
