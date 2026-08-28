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
import java.util.Properties;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for the PostgresDdlExecutor class, which provides DDL execution abilities for PostgreSQL
 * tables through Calcite.
 */
class PostgresDdlExecutorTest extends PostgresContainerTest {

  @BeforeEach
  void setUp() throws SQLException {
    // Set ThreadLocal DataSource for PostgresDdlExecutor.INSTANCE to use
    PostgresDdlExecutor.setThreadLocalDataSource(dataSource());

    // Clean up any existing test tables
    try (Connection connection = dataSource().getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE IF EXISTS city CASCADE");
      statement.execute("DROP TABLE IF EXISTS population CASCADE");
      statement.execute("DROP MATERIALIZED VIEW IF EXISTS city_population CASCADE");
    }
  }

  @AfterEach
  void tearDown() {
    // Clean up thread local storage
    PostgresDdlExecutor.clearThreadLocalDataSource();
  }

  @Test
  @Tag("integration")
  void testMaterializedView() throws SQLException {
    // Setup Calcite connection properties
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");
    info.setProperty("caseSensitive", "false");
    info.setProperty("unquotedCasing", "TO_LOWER");
    info.setProperty("quotedCasing", "TO_LOWER");
    info.setProperty("parserFactory", PostgresDdlExecutor.class.getName() + "#PARSER_FACTORY");
    info.setProperty("materializationsEnabled", "true");

    // Create test data in PostgreSQL first
    createTestData();

    // Now test with Calcite and PostgresDdlExecutor
    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Add schema and tables to Calcite
      try {
        // Create PostgreSQL tables in Calcite schema
        PostgresModifiableTable cityTable = new PostgresModifiableTable(dataSource(), "city");
        PostgresModifiableTable populationTable =
            new PostgresModifiableTable(dataSource(), "population");

        // Add tables to schema
        rootSchema.add("city", cityTable);
        rootSchema.add("population", populationTable);

        // Create a materialized view
        String mv = "CREATE MATERIALIZED VIEW city_population AS "
            + "SELECT c.id, c.name, c.geometry, p.population "
            + "FROM city c "
            + "JOIN population p ON c.id = p.city_id";

        // Execute the DDL statement to create the materialized view
        try (Statement statement = connection.createStatement()) {
          statement.execute(mv);
        }

        // Query the materialized view
        String sql = "SELECT id, name, population FROM city_population ORDER BY id";

        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql)) {

          // Verify the first row (Paris)
          assertTrue(resultSet.next());
          assertEquals(1, resultSet.getInt("id"));
          assertEquals("Paris", resultSet.getString("name"));
          assertEquals(2161000, resultSet.getInt("population"));

          // Verify the second row (New York)
          assertTrue(resultSet.next());
          assertEquals(2, resultSet.getInt("id"));
          assertEquals("New York", resultSet.getString("name"));
          assertEquals(8336000, resultSet.getInt("population"));

          // No more rows
          assertFalse(resultSet.next());
        }

        // Verify that the materialized view was created in PostgreSQL
        try (Connection pgConnection = dataSource().getConnection();
            Statement statement = pgConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'city_population')")) {
          assertTrue(resultSet.next());
          assertTrue(resultSet.getBoolean(1));
        }
      } finally {
        // Clean up
        try (Connection pgConnection = dataSource().getConnection();
            Statement statement = pgConnection.createStatement()) {
          statement.execute("DROP MATERIALIZED VIEW IF EXISTS city_population CASCADE");
          statement.execute("DROP TABLE IF EXISTS city CASCADE");
          statement.execute("DROP TABLE IF EXISTS population CASCADE");
        }
      }
    }
  }

  private void createTestData() throws SQLException {
    try (Connection connection = dataSource().getConnection();
        Statement statement = connection.createStatement()) {

      // Create the city table in PostgreSQL with PostGIS extension
      statement.execute("CREATE EXTENSION IF NOT EXISTS postgis");
      statement.execute("CREATE TABLE city (" +
          "id INTEGER PRIMARY KEY, " +
          "name VARCHAR(255), " +
          "geometry GEOMETRY)");

      // Create the population table
      statement.execute("CREATE TABLE population (" +
          "city_id INTEGER REFERENCES city(id), " +
          "population INTEGER)");

      // Insert Paris
      String parisWKT = "POINT(2.3522 48.8566)";
      try (PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO city VALUES (1, 'Paris', ST_GeomFromText(?, 4326))")) {
        ps.setString(1, parisWKT);
        ps.executeUpdate();
      }

      // Insert New York
      String nyWKT = "POINT(-74.0060 40.7128)";
      try (PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO city VALUES (2, 'New York', ST_GeomFromText(?, 4326))")) {
        ps.setString(1, nyWKT);
        ps.executeUpdate();
      }

      // Insert population data
      statement.execute("INSERT INTO population VALUES (1, 2161000)");
      statement.execute("INSERT INTO population VALUES (2, 8336000)");
    }
  }

  @Test
  @Tag("integration")
  void testCreateAndDropTable() throws SQLException {
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

      try {
        // Test CREATE TABLE
        try (Statement statement = connection.createStatement()) {
          statement.execute("CREATE TABLE test_table (id INTEGER, name VARCHAR)");
        }

        // Verify table exists in PostgreSQL
        try (Connection pgConnection = dataSource().getConnection();
            Statement statement = pgConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'test_table')")) {
          assertTrue(resultSet.next());
          assertTrue(resultSet.getBoolean(1));
        }

        // Add data
        try (Connection pgConnection = dataSource().getConnection();
            PreparedStatement ps = pgConnection.prepareStatement(
                "INSERT INTO test_table VALUES (1, 'Test Name')")) {
          ps.executeUpdate();
        }

        // Register table with Calcite
        PostgresModifiableTable testTable = new PostgresModifiableTable(dataSource(), "test_table");
        rootSchema.add("test_table", testTable);

        // Query the table through Calcite
        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM test_table")) {
          assertTrue(resultSet.next());
          assertEquals(1, resultSet.getInt("id"));
          assertEquals("Test Name", resultSet.getString("name"));
        }

        // Test DROP TABLE
        try (Statement statement = connection.createStatement()) {
          statement.execute("DROP TABLE test_table");
        }

        // Verify table no longer exists
        try (Connection pgConnection = dataSource().getConnection();
            Statement statement = pgConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'test_table')")) {
          assertTrue(resultSet.next());
          assertFalse(resultSet.getBoolean(1));
        }
      } finally {
        // Clean up
        try (Connection pgConnection = dataSource().getConnection();
            Statement statement = pgConnection.createStatement()) {
          statement.execute("DROP TABLE IF EXISTS test_table");
        }
      }
    }
  }

  @Test
  @Tag("integration")
  void testCreateAndDropView() throws SQLException {
    // Setup test data
    createTestData();

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

      try {
        // Register tables with Calcite
        PostgresModifiableTable cityTable = new PostgresModifiableTable(dataSource(), "city");
        PostgresModifiableTable populationTable =
            new PostgresModifiableTable(dataSource(), "population");
        rootSchema.add("city", cityTable);
        rootSchema.add("population", populationTable);

        // Test CREATE VIEW
        try (Statement statement = connection.createStatement()) {
          statement.execute("CREATE VIEW city_view AS " +
              "SELECT c.id, c.name, p.population " +
              "FROM city c JOIN population p ON c.id = p.city_id");
        }

        // Verify view exists in PostgreSQL
        try (Connection pgConnection = dataSource().getConnection();
            Statement statement = pgConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT EXISTS (SELECT 1 FROM information_schema.views WHERE table_name = 'city_view')")) {
          assertTrue(resultSet.next());
          assertTrue(resultSet.getBoolean(1));
        }

        // Query the view through Calcite
        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM city_view ORDER BY id")) {

          // Verify the first row (Paris)
          assertTrue(resultSet.next());
          assertEquals(1, resultSet.getInt("id"));
          assertEquals("Paris", resultSet.getString("name"));
          assertEquals(2161000, resultSet.getInt("population"));

          // Verify the second row (New York)
          assertTrue(resultSet.next());
          assertEquals(2, resultSet.getInt("id"));
          assertEquals("New York", resultSet.getString("name"));
          assertEquals(8336000, resultSet.getInt("population"));
        }

        // Test DROP VIEW
        try (Statement statement = connection.createStatement()) {
          statement.execute("DROP VIEW city_view");
        }

        // Verify view no longer exists
        try (Connection pgConnection = dataSource().getConnection();
            Statement statement = pgConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT EXISTS (SELECT 1 FROM information_schema.views WHERE table_name = 'city_view')")) {
          assertTrue(resultSet.next());
          assertFalse(resultSet.getBoolean(1));
        }
      } finally {
        // Clean up
        try (Connection pgConnection = dataSource().getConnection();
            Statement statement = pgConnection.createStatement()) {
          statement.execute("DROP VIEW IF EXISTS city_view");
          statement.execute("DROP TABLE IF EXISTS population CASCADE");
          statement.execute("DROP TABLE IF EXISTS city CASCADE");
        }
      }
    }
  }
}
