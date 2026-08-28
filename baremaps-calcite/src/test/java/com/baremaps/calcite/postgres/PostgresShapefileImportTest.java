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

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.calcite.shapefile.ShapefileTable;
import com.baremaps.testing.PostgresContainerTest;
import com.baremaps.testing.TestFiles;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests importing a shapefile into a PostgreSQL database using PostgresDdlExecutor.
 */
class PostgresShapefileImportTest extends PostgresContainerTest {

  private static final File SAMPLE_SHAPEFILE = TestFiles.POINT_SHP.toFile();
  private static final String SHAPEFILE_TABLE_NAME = "shapefile_data";
  private static final String IMPORTED_TABLE_NAME = "imported_shapefile";

  @BeforeEach
  void setUp() throws SQLException {
    // Set ThreadLocal DataSource for PostgresDdlExecutor.INSTANCE to use
    PostgresDdlExecutor.setThreadLocalDataSource(dataSource());

    // Clean up any existing test tables
    try (Connection connection = dataSource().getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE IF EXISTS " + SHAPEFILE_TABLE_NAME + " CASCADE");
      statement.execute("DROP TABLE IF EXISTS " + IMPORTED_TABLE_NAME + " CASCADE");
    }
  }

  @AfterEach
  void tearDown() {
    // Clean up thread local storage
    PostgresDdlExecutor.clearThreadLocalDataSource();
  }

  @Test
  @Tag("integration")
  void testImportShapefileToPostgres() throws Exception {
    // Setup Calcite connection properties
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");
    info.setProperty("caseSensitive", "false");
    info.setProperty("unquotedCasing", "TO_LOWER");
    info.setProperty("quotedCasing", "TO_LOWER");
    info.setProperty("parserFactory", PostgresDdlExecutor.class.getName() + "#PARSER_FACTORY");

    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Create a ShapefileTable instance
      ShapefileTable shapefileTable = new ShapefileTable(SAMPLE_SHAPEFILE);

      // Register the shapefile table in the Calcite schema
      rootSchema.add(SHAPEFILE_TABLE_NAME, shapefileTable);

      // Create a table in PostgreSQL by selecting from the shapefile table
      String createTableSql = "CREATE TABLE " + IMPORTED_TABLE_NAME + " AS " +
          "SELECT * FROM " + SHAPEFILE_TABLE_NAME;

      // Execute the DDL statement to create the table
      try (Statement statement = connection.createStatement()) {
        statement.execute(createTableSql);
      }

      // Verify that the table was created in PostgreSQL
      try (Connection pgConnection = dataSource().getConnection();
          Statement statement = pgConnection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = '" +
                  IMPORTED_TABLE_NAME + "')")) {
        assertTrue(resultSet.next());
        assertTrue(resultSet.getBoolean(1));
      }

      // Verify that the table has data
      try (Connection pgConnection = dataSource().getConnection();
          Statement statement = pgConnection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT COUNT(*) FROM " + IMPORTED_TABLE_NAME)) {
        assertTrue(resultSet.next());
        assertTrue(resultSet.getInt(1) > 0, "Table should have data");
      }

      // Verify that the table has a geometry column
      try (Connection pgConnection = dataSource().getConnection();
          Statement statement = pgConnection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT EXISTS (SELECT 1 FROM information_schema.columns " +
                  "WHERE table_name = '" + IMPORTED_TABLE_NAME + "' " +
                  "AND data_type = 'USER-DEFINED' AND udt_name = 'geometry')")) {
        assertTrue(resultSet.next());
        assertTrue(resultSet.getBoolean(1), "Table should have a geometry column");
      }

      // Query the imported table through Calcite
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT * FROM " + IMPORTED_TABLE_NAME + " LIMIT 5")) {
        assertTrue(resultSet.next(), "Should have at least one row");
      }
    }
  }
}
