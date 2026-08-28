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

import com.baremaps.calcite.data.DataTableSchema;
import com.baremaps.testing.PostgresContainerTest;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.ModifiableTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKBReader;

/**
 * Tests for the PostgisTable class, which provides access to PostgreSQL/PostGIS tables through the
 * Calcite framework for SQL querying.
 */
class PostgresModifiableTableTest extends PostgresContainerTest {

  private static final String TEST_TABLE = "postgis_test";

  @BeforeEach
  void setUp() throws SQLException {
    DataSource dataSource = dataSource();
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      // Ensure PostGIS extension is available
      statement.execute("CREATE EXTENSION IF NOT EXISTS postgis");

      // Drop the test table if it exists
      statement.execute("DROP TABLE IF EXISTS " + TEST_TABLE);

      // Create a test table with various data types including a geometry column
      statement.execute("CREATE TABLE " + TEST_TABLE + " (" +
          "id INTEGER PRIMARY KEY, " +
          "name VARCHAR(100), " +
          "description TEXT, " +
          "is_active BOOLEAN, " +
          "height DOUBLE PRECISION, " +
          "created_at TIMESTAMP, " +
          "geometry GEOMETRY(Point, 4326)" +
          ")");

      // Insert test data
      statement.execute("INSERT INTO " + TEST_TABLE +
          " VALUES (1, 'Point 1', 'First test point', true, 123.45, NOW(), " +
          "ST_SetSRID(ST_MakePoint(10.5, 20.5), 4326))");
      statement.execute("INSERT INTO " + TEST_TABLE +
          " VALUES (2, 'Point 2', 'Second test point', false, 67.89, NOW(), " +
          "ST_SetSRID(ST_MakePoint(-5.5, 30.2), 4326))");
    }
  }

  @Nested
  class SchemaTests {

    @Test
    @Tag("integration")
    void schemaContainsExpectedColumns() throws Exception {
      PostgresModifiableTable table = new PostgresModifiableTable(dataSource(), TEST_TABLE);
      DataTableSchema schema = table.schema();

      assertNotNull(schema, "Schema should not be null");
      assertEquals(TEST_TABLE, schema.name(), "Schema should have correct name");
      assertEquals(7, schema.columns().size(), "Schema should have 7 columns");

      // Verify column names
      assertTrue(schema.columns().stream().anyMatch(c -> c.name().equals("id")),
          "Schema should have 'id' column");
      assertTrue(schema.columns().stream().anyMatch(c -> c.name().equals("name")),
          "Schema should have 'name' column");
      assertTrue(schema.columns().stream().anyMatch(c -> c.name().equals("geometry")),
          "Schema should have 'geometry' column");
    }

    @Test
    @Tag("integration")
    void rowTypeMatchesSchema() throws Exception {
      RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
      PostgresModifiableTable table =
          new PostgresModifiableTable(dataSource(), TEST_TABLE, typeFactory);
      RelDataType rowType = table.getRowType(typeFactory);

      assertNotNull(rowType, "Row type should not be null");
      assertEquals(7, rowType.getFieldCount(), "Row type should have 7 fields");

      // Verify the field names in the row type
      assertTrue(rowType.getFieldNames().contains("id"), "Row type should contain 'id' field");
      assertTrue(rowType.getFieldNames().contains("geometry"),
          "Row type should contain 'geometry' field");
    }
  }

  @Nested
  class SqlQueryTests {

    private Connection connection;
    private CalciteConnection calciteConnection;

    @BeforeEach
    void setUpCalciteConnection() throws Exception {
      Class.forName("org.apache.calcite.jdbc.Driver");
      Properties info = new Properties();
      info.setProperty("lex", "JAVA");
      info.setProperty("quoting", "DOUBLE_QUOTE");

      connection = DriverManager.getConnection("jdbc:calcite:", info);
      calciteConnection = connection.unwrap(CalciteConnection.class);

      // Add our PostGIS table to the schema
      PostgresModifiableTable postgresModifiableTable =
          new PostgresModifiableTable(dataSource(), TEST_TABLE);
      calciteConnection.getRootSchema().add(TEST_TABLE, postgresModifiableTable);
    }

    @Test
    @Tag("integration")
    void simpleSelectReturnsExpectedResults() throws Exception {
      try (Statement statement = calciteConnection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT id, name, is_active FROM " + TEST_TABLE + " ORDER BY id")) {

        // Verify first row
        assertTrue(resultSet.next(), "Should have at least one row");
        assertEquals(1, resultSet.getInt("id"));
        assertEquals("Point 1", resultSet.getString("name"));
        assertTrue(resultSet.getBoolean("is_active"));

        // Verify second row
        assertTrue(resultSet.next(), "Should have a second row");
        assertEquals(2, resultSet.getInt("id"));
        assertEquals("Point 2", resultSet.getString("name"));
        assertFalse(resultSet.getBoolean("is_active"));

        // No more rows
        assertFalse(resultSet.next(), "Should have only two rows");
      }
    }

    @Test
    @Tag("integration")
    void selectWithGeometryColumn() throws Exception {
      try (Statement statement = calciteConnection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT id, name, geometry FROM " + TEST_TABLE + " ORDER BY id")) {

        // Verify first row with geometry
        assertTrue(resultSet.next(), "Should have at least one row");
        assertEquals(1, resultSet.getInt("id"));
        Object geometry = resultSet.getObject("geometry");
        assertNotNull(geometry, "Geometry should not be null");
        assertTrue(geometry instanceof Geometry, "Should get a JTS Geometry object");
        assertTrue(geometry instanceof Point, "Should be a Point geometry");

        // Verify second row with geometry
        assertTrue(resultSet.next(), "Should have a second row");
        assertEquals(2, resultSet.getInt("id"));
        geometry = resultSet.getObject("geometry");
        assertNotNull(geometry, "Geometry should not be null");
        assertTrue(geometry instanceof Geometry, "Should get a JTS Geometry object");
        assertTrue(geometry instanceof Point, "Should be a Point geometry");
      }
    }

    @Test
    @Tag("integration")
    void filteringByColumns() throws Exception {
      try (Statement statement = calciteConnection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT id, name FROM " + TEST_TABLE + " WHERE is_active = true")) {

        // Should only have the first row
        assertTrue(resultSet.next(), "Should have one matching row");
        assertEquals(1, resultSet.getInt("id"));
        assertEquals("Point 1", resultSet.getString("name"));

        // No more rows
        assertFalse(resultSet.next(), "Should have only one matching row");
      }
    }
  }

  @Nested
  class ModifiableTableTests {

    private PostgresModifiableTable table;
    private ModifiableTable modifiableTable;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() throws SQLException {
      table = new PostgresModifiableTable(dataSource(), TEST_TABLE);
      modifiableTable = (ModifiableTable) table;
      geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    }

    @Test
    @Tag("integration")
    void insertSingleRow() throws Exception {
      // Create a new point geometry
      Point point =
          geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(15.0, 25.0));

      // Create a row with values matching the table schema
      Object[] row = new Object[] {
          3, // id
          "Point 3", // name
          "Third test point", // description
          true, // is_active
          45.67, // height
          java.time.LocalDateTime.now(), // created_at - using LocalDateTime instead of Timestamp
          point // geometry
      };

      // Get the modifiable collection and add the row
      Collection<Object[]> collection = modifiableTable.getModifiableCollection();
      boolean added = collection.add(row);

      assertTrue(added, "Row should be added successfully");

      // Verify the row was added by querying the database
      try (Connection connection = dataSource().getConnection();
          Statement stmt = connection.createStatement();
          ResultSet rs = stmt.executeQuery("SELECT * FROM " + TEST_TABLE + " WHERE id = 3")) {

        assertTrue(rs.next(), "Should find the inserted row");
        assertEquals(3, rs.getInt("id"));
        assertEquals("Point 3", rs.getString("name"));
        assertEquals("Third test point", rs.getString("description"));
        assertTrue(rs.getBoolean("is_active"));
        assertEquals(45.67, rs.getDouble("height"), 0.01);

        // Check geometry
        String wkb = rs.getString("geometry");
        assertNotNull(wkb, "Geometry should not be null");
        WKBReader reader = new WKBReader(geometryFactory);
        Geometry geometry = reader.read(WKBReader.hexToBytes(wkb));

        assertTrue(geometry instanceof Point, "Should be a Point geometry");
        Point resultPoint = (Point) geometry;
        assertEquals(15.0, resultPoint.getX(), 0.01);
        assertEquals(25.0, resultPoint.getY(), 0.01);
        assertEquals(4326, geometry.getSRID(), "SRID should be 4326");
      }
    }

    @Test
    @Tag("integration")
    void insertMultipleRowsUsingCopyApi() throws Exception {
      // Create multiple rows to insert
      List<Object[]> rows = new ArrayList<>();

      // Row 1
      rows.add(new Object[] {
          4, // id
          "Point 4", // name
          "Fourth test point", // description
          true, // is_active
          12.34, // height
          java.time.LocalDateTime.now(), // created_at - using LocalDateTime instead of Timestamp
          geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(30.0, 40.0)) // geometry
      });

      // Row 2
      rows.add(new Object[] {
          5, // id
          "Point 5", // name
          "Fifth test point", // description
          false, // is_active
          56.78, // height
          java.time.LocalDateTime.now(), // created_at - using LocalDateTime instead of Timestamp
          geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(-20.0, 50.0)) // geometry
      });

      // Get the modifiable collection and add all rows
      Collection<Object[]> collection = modifiableTable.getModifiableCollection();
      boolean added = collection.addAll(rows);

      assertTrue(added, "Rows should be added successfully");

      // Verify the rows were added by querying the database
      try (Connection connection = dataSource().getConnection();
          Statement stmt = connection.createStatement();
          ResultSet rs = stmt
              .executeQuery("SELECT * FROM " + TEST_TABLE + " WHERE id IN (4, 5) ORDER BY id")) {

        // Check first row
        assertTrue(rs.next(), "Should find the first inserted row");
        assertEquals(4, rs.getInt("id"));
        assertEquals("Point 4", rs.getString("name"));
        assertEquals("Fourth test point", rs.getString("description"));
        assertTrue(rs.getBoolean("is_active"));
        assertEquals(12.34, rs.getDouble("height"), 0.01);

        // Check second row
        assertTrue(rs.next(), "Should find the second inserted row");
        assertEquals(5, rs.getInt("id"));
        assertEquals("Point 5", rs.getString("name"));
        assertEquals("Fifth test point", rs.getString("description"));
        assertFalse(rs.getBoolean("is_active"));
        assertEquals(56.78, rs.getDouble("height"), 0.01);

        // No more rows
        assertFalse(rs.next(), "Should have only two rows");
      }
    }

    @Test
    @Tag("integration")
    void clearTable() throws Exception {
      // Get the modifiable collection and clear it
      Collection<Object[]> collection = modifiableTable.getModifiableCollection();
      collection.clear();

      // Verify the table is empty
      try (Connection connection = dataSource().getConnection();
          Statement stmt = connection.createStatement();
          ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + TEST_TABLE)) {

        assertTrue(rs.next(), "Should have a result");
        assertEquals(0, rs.getInt(1), "Table should be empty");
      }
    }
  }
}
