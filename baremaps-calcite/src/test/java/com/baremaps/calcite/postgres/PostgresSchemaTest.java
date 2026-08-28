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

import com.baremaps.testing.PostgresContainerTest;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for the PostgresSchema class, which provides access to PostgreSQL tables through the
 * Calcite framework.
 */
class PostgresSchemaTest extends PostgresContainerTest {

  private static final String TEST_TABLE = "test_table";
  private static final String TEST_SCHEMA = "public";
  private DataSource dataSource;
  private RelDataTypeFactory typeFactory;

  @BeforeEach
  void setUp() throws SQLException {
    dataSource = dataSource();
    typeFactory = new JavaTypeFactoryImpl();

    // Create a test table
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      // Ensure PostGIS extension is available
      statement.execute("CREATE EXTENSION IF NOT EXISTS postgis");

      statement.execute("DROP TABLE IF EXISTS " + TEST_TABLE);
      statement.execute("CREATE TABLE " + TEST_TABLE + " (" +
          "id INTEGER PRIMARY KEY, " +
          "name VARCHAR(100), " +
          "description TEXT" +
          ")");

      // Insert some test data
      statement.execute("INSERT INTO " + TEST_TABLE + " VALUES (1, 'Test 1', 'Description 1')");
      statement.execute("INSERT INTO " + TEST_TABLE + " VALUES (2, 'Test 2', 'Description 2')");
    }
  }

  @Test
  @Tag("integration")
  void testGetTableMap() throws SQLException {
    // Create a PostgresSchema instance
    PostgresSchema schema = new PostgresSchema(dataSource, TEST_SCHEMA, typeFactory);

    // Get the table map
    Map<String, Table> tableMap = schema.getTableMap();

    // Verify that our test table is in the map
    assertTrue(tableMap.containsKey(TEST_TABLE), "Table map should contain test table");

    // Verify that we can query the table through Calcite
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");
    info.setProperty("caseSensitive", "false");

    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Add our schema to the root schema
      rootSchema.add(TEST_SCHEMA, schema);

      // Query the table
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT * FROM " + TEST_SCHEMA + "." + TEST_TABLE + " ORDER BY id")) {

        // Verify first row
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.getInt("id"));
        assertEquals("Test 1", resultSet.getString("name"));
        assertEquals("Description 1", resultSet.getString("description"));

        // Verify second row
        assertTrue(resultSet.next());
        assertEquals(2, resultSet.getInt("id"));
        assertEquals("Test 2", resultSet.getString("name"));
        assertEquals("Description 2", resultSet.getString("description"));

        // Verify no more rows
        assertFalse(resultSet.next());
      }
    }
  }
}
