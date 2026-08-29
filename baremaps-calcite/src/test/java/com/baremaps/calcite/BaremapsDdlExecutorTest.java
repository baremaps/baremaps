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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.calcite.data.DataModifiableTable;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.type.SqlTypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

/** Tests the DDL statements executed against in-memory {@link DataModifiableTable}s. */
class BaremapsDdlExecutorTest {

  @TempDir
  Path tempDir;

  private final RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();

  private DataModifiableTable city;
  private DataModifiableTable population;
  private DataModifiableTable testTable;

  @BeforeEach
  void setUp() {
    GeometryFactory geometryFactory = new GeometryFactory();
    city = DataModifiableTable.inMemory("city", typeFactory.builder()
        .add("id", SqlTypeName.INTEGER)
        .add("name", SqlTypeName.VARCHAR)
        .add("geometry", SqlTypeName.GEOMETRY)
        .build());
    city.rows().add(new Object[] {1, "Paris",
        geometryFactory.createPoint(new Coordinate(2.3522, 48.8566))});
    city.rows().add(new Object[] {2, "New York",
        geometryFactory.createPoint(new Coordinate(-74.0060, 40.7128))});

    population = DataModifiableTable.inMemory("population", typeFactory.builder()
        .add("city_id", SqlTypeName.INTEGER)
        .add("population", SqlTypeName.INTEGER)
        .build());
    population.rows().add(new Object[] {1, 2_161_000});
    population.rows().add(new Object[] {2, 8_336_000});

    testTable = DataModifiableTable.inMemory("test_table", typeFactory.builder()
        .add("id", SqlTypeName.INTEGER)
        .add("name", SqlTypeName.VARCHAR)
        .build());
    testTable.rows().add(new Object[] {1, "Test Name"});
  }

  private Connection connect() throws SQLException {
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");
    info.setProperty("caseSensitive", "false");
    info.setProperty("unquotedCasing", "TO_LOWER");
    info.setProperty("quotedCasing", "TO_LOWER");
    info.setProperty("parserFactory", BaremapsDdlExecutor.class.getName() + "#PARSER_FACTORY");
    info.setProperty("materializationsEnabled", "true");
    Connection connection = DriverManager.getConnection("jdbc:calcite:", info);
    SchemaPlus rootSchema = connection.unwrap(CalciteConnection.class).getRootSchema();
    rootSchema.add("city", city);
    rootSchema.add("population", population);
    rootSchema.add("test_table", testTable);
    return connection;
  }

  private static void execute(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private static void assertCityPopulation(Connection connection, String table)
      throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(
            "SELECT id, name, population FROM " + table + " ORDER BY id")) {
      assertTrue(resultSet.next());
      assertEquals(1, resultSet.getInt("id"));
      assertEquals("Paris", resultSet.getString("name"));
      assertEquals(2_161_000, resultSet.getInt("population"));
      assertTrue(resultSet.next());
      assertEquals(2, resultSet.getInt("id"));
      assertEquals("New York", resultSet.getString("name"));
      assertEquals(8_336_000, resultSet.getInt("population"));
      assertFalse(resultSet.next());
    }
  }

  private static void assertNotFound(Connection connection, String table) {
    SQLException e = assertThrows(SQLException.class, () -> {
      try (Statement statement = connection.createStatement()) {
        statement.executeQuery("SELECT * FROM " + table);
      }
    });
    assertTrue(e.getMessage().contains("not found"));
  }

  @Test
  @Tag("integration")
  void materializedView() throws SQLException {
    try (Connection connection = connect()) {
      execute(connection, "CREATE MATERIALIZED VIEW city_population AS "
          + "SELECT c.id, c.name, c.geometry, p.population "
          + "FROM city c JOIN population p ON c.id = p.city_id");
      assertCityPopulation(connection, "city_population");

      execute(connection, "DROP MATERIALIZED VIEW city_population");
      assertNotFound(connection, "city_population");
    }
  }

  @Test
  @Tag("integration")
  void createAndDropTable() throws SQLException {
    try (Connection connection = connect()) {
      execute(connection, "CREATE TABLE new_table (id INTEGER, name VARCHAR)");
      execute(connection, "INSERT INTO new_table VALUES (1, 'New Table Name')");
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT * FROM new_table")) {
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.getInt("id"));
        assertEquals("New Table Name", resultSet.getString("name"));
      }

      execute(connection, "DROP TABLE new_table");
      assertNotFound(connection, "new_table");
    }
  }

  @Test
  @Tag("integration")
  void createAndDropView() throws SQLException {
    try (Connection connection = connect()) {
      execute(connection, "CREATE VIEW city_view AS "
          + "SELECT c.id, c.name, p.population "
          + "FROM city c JOIN population p ON c.id = p.city_id");
      assertCityPopulation(connection, "city_view");

      execute(connection, "DROP VIEW city_view");
      assertNotFound(connection, "city_view");
    }
  }

  @Test
  @Tag("integration")
  void truncateTable() throws SQLException {
    try (Connection connection = connect()) {
      execute(connection, "TRUNCATE TABLE test_table");
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT * FROM test_table")) {
        assertFalse(resultSet.next());
      }
    }
  }

  @Test
  @Tag("integration")
  void createTableInDirectory() throws Exception {
    Path directory = tempDir.resolve("persisted");
    try (Connection connection = connect()) {
      execute(connection, "CREATE TABLE persisted AS SELECT id, name FROM test_table "
          + "WITH (format = 'data', file = '" + directory + "')");
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT * FROM persisted")) {
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.getInt("id"));
        assertEquals("Test Name", resultSet.getString("name"));
      }
      // Persist the row count so that the directory can be reopened
      ((DataModifiableTable) connection.unwrap(CalciteConnection.class).getRootSchema()
          .getTable("persisted")).close();
    }

    try (DataModifiableTable reopened = DataModifiableTable.open(directory, typeFactory)) {
      RelDataType rowType = reopened.getRowType(typeFactory);
      assertEquals(2, rowType.getFieldCount());
      assertEquals("id", rowType.getFieldNames().get(0));
      assertEquals(1, reopened.rows().size());
      assertEquals("Test Name", reopened.rows().iterator().next()[1]);
    }
  }

  @Test
  @Tag("integration")
  void createTableFromFormat() throws Exception {
    try (Connection connection = connect()) {
      execute(connection, "CREATE TABLE shapefile (id INTEGER) WITH (format = 'shp', file = '"
          + com.baremaps.testing.TestFiles.POINT_SHP + "')");
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM shapefile")) {
        assertTrue(resultSet.next());
        assertTrue(resultSet.getInt(1) > 0);
      }
    }
  }
}
