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

package com.baremaps.calcite;

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.calcite.data.*;
import com.baremaps.data.collection.AppendOnlyLog;
import com.baremaps.data.collection.DataCollection;
import com.baremaps.data.memory.MemoryMappedDirectory;
import com.baremaps.testing.FileUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;
import java.util.Properties;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * Tests for the BaremapsDdlExecutor class, which provides DDL execution abilities for Baremaps
 * tables through Calcite.
 */
public class BaremapsDdlExecutorTest {

  @TempDir
  Path cityDataDir;

  @TempDir
  Path cityPopulationDir;

  @TempDir
  Path populationDataDir;

  @TempDir
  Path testTableDataDir;

  private DataCollection<DataRow> cityCollection;
  private DataCollection<DataRow> populationCollection;
  private DataCollection<DataRow> testTableCollection;
  private DataTableSchema citySchema;
  private DataTableSchema populationSchema;
  private DataTableSchema testTableSchema;
  private RelDataTypeFactory typeFactory;

  @BeforeEach
  void setUp() throws IOException {
    // Initialize type factory
    typeFactory = new JavaTypeFactoryImpl();

    // Create schemas
    citySchema = createCitySchema();
    populationSchema = createPopulationSchema();
    testTableSchema = createTestTableSchema();

    // Create and initialize collections
    MemoryMappedDirectory cityMemory = new MemoryMappedDirectory(cityDataDir);
    DataRowType cityRowType = new DataRowType(citySchema);
    cityCollection = AppendOnlyLog.<DataRow>builder()
        .dataType(cityRowType)
        .memory(cityMemory)
        .build();

    MemoryMappedDirectory populationMemory =
        new MemoryMappedDirectory(populationDataDir);
    DataRowType populationRowType = new DataRowType(populationSchema);
    populationCollection = AppendOnlyLog.<DataRow>builder()
        .dataType(populationRowType)
        .memory(populationMemory)
        .build();

    MemoryMappedDirectory testTableMemory =
        new MemoryMappedDirectory(testTableDataDir);
    DataRowType testTableRowType = new DataRowType(testTableSchema);
    testTableCollection = AppendOnlyLog.<DataRow>builder()
        .dataType(testTableRowType)
        .memory(testTableMemory)
        .build();
  }

  @AfterEach
  void tearDown() throws Exception {
    // Clean up any additional directories created during test execution
    FileUtils.deleteRecursively(Paths.get("options_table").toFile());
    FileUtils.deleteRecursively(Paths.get("options_table_as").toFile());
    FileUtils.deleteRecursively(Paths.get("new_table").toFile());
    FileUtils.deleteRecursively(Paths.get("city_view").toFile());
    FileUtils.deleteRecursively(Paths.get("city_population").toFile());
    FileUtils.deleteRecursively(Paths.get("test_table").toFile());
  }

  private DataTableSchema createCitySchema() {
    return new DataTableSchema("city", List.of(
        new DataColumnFixed("id", DataColumn.Cardinality.REQUIRED,
            typeFactory.createSqlType(org.apache.calcite.sql.type.SqlTypeName.INTEGER)),
        new DataColumnFixed("name", DataColumn.Cardinality.OPTIONAL,
            typeFactory.createSqlType(org.apache.calcite.sql.type.SqlTypeName.VARCHAR)),
        new DataColumnFixed("geometry", DataColumn.Cardinality.OPTIONAL,
            typeFactory.createSqlType(org.apache.calcite.sql.type.SqlTypeName.GEOMETRY))));
  }

  private DataTableSchema createPopulationSchema() {
    return new DataTableSchema("population", List.of(
        new DataColumnFixed("city_id", DataColumn.Cardinality.REQUIRED,
            typeFactory.createSqlType(org.apache.calcite.sql.type.SqlTypeName.INTEGER)),
        new DataColumnFixed("population", DataColumn.Cardinality.OPTIONAL,
            typeFactory.createSqlType(org.apache.calcite.sql.type.SqlTypeName.INTEGER))));
  }

  private DataTableSchema createTestTableSchema() {
    return new DataTableSchema("test_table", List.of(
        new DataColumnFixed("id", DataColumn.Cardinality.REQUIRED,
            typeFactory.createSqlType(org.apache.calcite.sql.type.SqlTypeName.INTEGER)),
        new DataColumnFixed("name", DataColumn.Cardinality.OPTIONAL,
            typeFactory.createSqlType(org.apache.calcite.sql.type.SqlTypeName.VARCHAR))));
  }

  /**
   * Helper method to set up Calcite connection with BaremapsDdlExecutor
   */
  private Connection createCalciteConnection() throws SQLException {
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");
    info.setProperty("caseSensitive", "false");
    info.setProperty("unquotedCasing", "TO_LOWER");
    info.setProperty("quotedCasing", "TO_LOWER");
    info.setProperty("parserFactory", BaremapsDdlExecutor.class.getName() + "#PARSER_FACTORY");
    info.setProperty("materializationsEnabled", "true");

    return DriverManager.getConnection("jdbc:calcite:", info);
  }

  /**
   * Helper method to populate test data
   */
  private void populateTestData() {
    GeometryFactory geometryFactory = new GeometryFactory();

    // Add data to the city table
    Point parisPoint = geometryFactory.createPoint(new Coordinate(2.3522, 48.8566));
    Point nyPoint = geometryFactory.createPoint(new Coordinate(-74.0060, 40.7128));

    cityCollection.add(new DataRow(citySchema, List.of(1, "Paris", parisPoint)));
    cityCollection.add(new DataRow(citySchema, List.of(2, "New York", nyPoint)));

    // Add data to the population table
    populationCollection.add(new DataRow(populationSchema, List.of(1, 2_161_000)));
    populationCollection.add(new DataRow(populationSchema, List.of(2, 8_336_000)));

    // Add data to the test table
    testTableCollection.add(new DataRow(testTableSchema, List.of(1, "Test Name")));
  }

  @Test
  @Tag("integration")
  void testMaterializedView() throws SQLException {
    // Set up test data
    populateTestData();

    try (Connection connection = createCalciteConnection()) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Create the city table
      DataModifiableTable cityTable = new DataModifiableTable(
          "city",
          citySchema,
          cityCollection,
          typeFactory);

      // Add city table to the schema
      rootSchema.add("city", cityTable);

      // Create the population table
      DataModifiableTable populationTable = new DataModifiableTable(
          "population",
          populationSchema,
          populationCollection,
          typeFactory);

      // Add population table to the schema
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
        assertEquals(2_161_000, resultSet.getInt("population"));

        // Verify the second row (New York)
        assertTrue(resultSet.next());
        assertEquals(2, resultSet.getInt("id"));
        assertEquals("New York", resultSet.getString("name"));
        assertEquals(8_336_000, resultSet.getInt("population"));

        // No more rows
        assertFalse(resultSet.next());
      }
    }
  }

  @Test
  @Tag("integration")
  void testCreateAndDropTable() throws SQLException {
    // Set up test data
    populateTestData();

    try (Connection connection = createCalciteConnection()) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Create the test table
      DataModifiableTable testTable = new DataModifiableTable(
          "test_table",
          testTableSchema,
          testTableCollection,
          typeFactory);

      // Add test table to the schema
      rootSchema.add("test_table", testTable);

      // Test CREATE TABLE
      try (Statement statement = connection.createStatement()) {
        statement.execute("CREATE TABLE new_table (id INTEGER, name VARCHAR)");
      }

      // Add data to the new table
      try (Statement statement = connection.createStatement()) {
        statement.execute("INSERT INTO new_table VALUES (1, 'New Table Name')");
      }

      // Query the new table
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT * FROM new_table")) {
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.getInt("id"));
        assertEquals("New Table Name", resultSet.getString("name"));
      }

      // Test DROP TABLE
      try (Statement statement = connection.createStatement()) {
        statement.execute("DROP TABLE new_table");
      }

      // Verify table no longer exists
      try (Statement statement = connection.createStatement()) {
        statement.executeQuery("SELECT * FROM new_table");
        fail("Table should have been dropped");
      } catch (SQLException e) {
        // Expected exception
        assertTrue(e.getMessage().contains("not found"));
      }
    }
  }

  @Test
  @Tag("integration")
  void testCreateAndDropView() throws SQLException {
    // Set up test data
    populateTestData();

    try (Connection connection = createCalciteConnection()) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Create the city table
      DataModifiableTable cityTable = new DataModifiableTable(
          "city",
          citySchema,
          cityCollection,
          typeFactory);

      // Add city table to the schema
      rootSchema.add("city", cityTable);

      // Create the population table
      DataModifiableTable populationTable = new DataModifiableTable(
          "population",
          populationSchema,
          populationCollection,
          typeFactory);

      // Add population table to the schema
      rootSchema.add("population", populationTable);

      // Test CREATE VIEW
      try (Statement statement = connection.createStatement()) {
        statement.execute("CREATE VIEW city_view AS " +
            "SELECT c.id, c.name, p.population " +
            "FROM city c JOIN population p ON c.id = p.city_id");
      }

      // Query the view
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT * FROM city_view ORDER BY id")) {

        // Verify the first row (Paris)
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.getInt("id"));
        assertEquals("Paris", resultSet.getString("name"));
        assertEquals(2_161_000, resultSet.getInt("population"));

        // Verify the second row (New York)
        assertTrue(resultSet.next());
        assertEquals(2, resultSet.getInt("id"));
        assertEquals("New York", resultSet.getString("name"));
        assertEquals(8_336_000, resultSet.getInt("population"));
      }

      // Test DROP VIEW
      try (Statement statement = connection.createStatement()) {
        statement.execute("DROP VIEW city_view");
      }

      // Verify view no longer exists
      try (Statement statement = connection.createStatement()) {
        statement.executeQuery("SELECT * FROM city_view");
        fail("View should have been dropped");
      } catch (SQLException e) {
        // Expected exception
        assertTrue(e.getMessage().contains("not found"));
      }
    }
  }

  @Test
  @Tag("integration")
  void testTruncateTable() throws SQLException {
    // Set up test data
    populateTestData();

    try (Connection connection = createCalciteConnection()) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Create the test table
      DataModifiableTable testTable = new DataModifiableTable(
          "test_table",
          testTableSchema,
          testTableCollection,
          typeFactory);

      // Add test table to the schema
      rootSchema.add("test_table", testTable);

      // Force data to be persisted by accessing it
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT * FROM test_table")) {
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.getInt("id"));
        assertEquals("Test Name", resultSet.getString("name"));
      }

      // Test TRUNCATE TABLE
      try (Statement statement = connection.createStatement()) {
        statement.execute("TRUNCATE TABLE test_table");
      }

      // Verify table is empty
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT * FROM test_table")) {
        assertFalse(resultSet.next());
      }
    }
  }

  @Test
  @Tag("integration")
  void testCreateTableWithOptions() throws SQLException {
    // Set up test data
    populateTestData();

    try (Connection connection = createCalciteConnection()) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Create the test table
      DataModifiableTable testTable = new DataModifiableTable(
          "test_table",
          testTableSchema,
          testTableCollection,
          typeFactory);

      // Add test table to the schema
      rootSchema.add("test_table", testTable);

      // Test CREATE TABLE with WITH options
      try (Statement statement = connection.createStatement()) {
        statement.execute("CREATE TABLE options_table (id INTEGER, name VARCHAR) " +
            "WITH (option1 = 'value1', option2 = 'value2')");
      }

      // Add data to the new table
      try (Statement statement = connection.createStatement()) {
        statement.execute("INSERT INTO options_table VALUES (1, 'Options Table Name')");
      }

      // Query the new table
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT * FROM options_table")) {
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.getInt("id"));
        assertEquals("Options Table Name", resultSet.getString("name"));
      }

      // Test CREATE TABLE AS with WITH options
      try (Statement statement = connection.createStatement()) {
        statement.execute("CREATE TABLE options_table_as AS " +
            "SELECT id, name FROM test_table " +
            "WITH (option3 = 'value3', option4 = 'value4')");
      }

      // Query the new table created with AS
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT * FROM options_table_as")) {
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.getInt("id"));
        assertEquals("Test Name", resultSet.getString("name"));
      }

      // Clean up
      try (Statement statement = connection.createStatement()) {
        statement.execute("DROP TABLE options_table");
        statement.execute("DROP TABLE options_table_as");
      }
    }
  }
}
