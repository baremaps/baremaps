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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.testing.PostgresContainerTest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.apache.calcite.jdbc.CalciteConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests the DDL statements executed against PostgreSQL through a registered {@link PostgresSchema}.
 */
class PostgresDdlExecutorTest extends PostgresContainerTest {

  @BeforeEach
  void setUp() throws SQLException {
    executeInPostgres(
        "CREATE EXTENSION IF NOT EXISTS postgis",
        "DROP MATERIALIZED VIEW IF EXISTS city_population CASCADE",
        "DROP VIEW IF EXISTS city_view",
        "DROP TABLE IF EXISTS population CASCADE",
        "DROP TABLE IF EXISTS city CASCADE",
        "DROP TABLE IF EXISTS test_table",
        "CREATE TABLE city (id INTEGER PRIMARY KEY, name VARCHAR(255), geometry GEOMETRY)",
        "CREATE TABLE population (city_id INTEGER REFERENCES city(id), population INTEGER)",
        "INSERT INTO city VALUES (1, 'Paris', ST_GeomFromText('POINT(2.3522 48.8566)', 4326))",
        "INSERT INTO city VALUES (2, 'New York', ST_GeomFromText('POINT(-74.0060 40.7128)', 4326))",
        "INSERT INTO population VALUES (1, 2161000)",
        "INSERT INTO population VALUES (2, 8336000)");
  }

  private void executeInPostgres(String... statements) throws SQLException {
    try (Connection connection = dataSource().getConnection();
        Statement statement = connection.createStatement()) {
      for (String sql : statements) {
        statement.execute(sql);
      }
    }
  }

  private boolean existsInPostgres(String query) throws SQLException {
    try (Connection connection = dataSource().getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT EXISTS (" + query + ")")) {
      return resultSet.next() && resultSet.getBoolean(1);
    }
  }

  /** Connects to Calcite with the {@code public} schema of the container as default schema. */
  private Connection connect() throws SQLException {
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");
    info.setProperty("caseSensitive", "false");
    info.setProperty("unquotedCasing", "TO_LOWER");
    info.setProperty("quotedCasing", "TO_LOWER");
    info.setProperty("parserFactory", PostgresDdlExecutor.class.getName() + "#PARSER_FACTORY");
    Connection connection = DriverManager.getConnection("jdbc:calcite:", info);
    CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
    calciteConnection.getRootSchema().add("pg",
        new PostgresSchema(dataSource(), "public", calciteConnection.getTypeFactory()));
    calciteConnection.setSchema("pg");
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
      assertEquals(2161000, resultSet.getInt("population"));
      assertTrue(resultSet.next());
      assertEquals(2, resultSet.getInt("id"));
      assertEquals("New York", resultSet.getString("name"));
      assertEquals(8336000, resultSet.getInt("population"));
      assertFalse(resultSet.next());
    }
  }

  @Test
  @Tag("integration")
  void materializedView() throws SQLException {
    try (Connection connection = connect()) {
      execute(connection, "CREATE MATERIALIZED VIEW city_population AS "
          + "SELECT c.id, c.name, c.geometry, p.population "
          + "FROM city c JOIN population p ON c.id = p.city_id");
      assertTrue(existsInPostgres(
          "SELECT 1 FROM pg_matviews WHERE matviewname = 'city_population'"));
      assertCityPopulation(connection, "city_population");

      execute(connection, "DROP MATERIALIZED VIEW city_population");
      assertFalse(existsInPostgres(
          "SELECT 1 FROM pg_matviews WHERE matviewname = 'city_population'"));
    }
  }

  @Test
  @Tag("integration")
  void createAndDropTable() throws SQLException {
    try (Connection connection = connect()) {
      execute(connection, "CREATE TABLE test_table (id INTEGER, name VARCHAR)");
      assertTrue(existsInPostgres(
          "SELECT 1 FROM information_schema.tables WHERE table_name = 'test_table'"));

      executeInPostgres("INSERT INTO test_table VALUES (1, 'Test Name')");
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT * FROM test_table")) {
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.getInt("id"));
        assertEquals("Test Name", resultSet.getString("name"));
      }

      execute(connection, "DROP TABLE test_table");
      assertFalse(existsInPostgres(
          "SELECT 1 FROM information_schema.tables WHERE table_name = 'test_table'"));
    }
  }

  @Test
  @Tag("integration")
  void createTableAsSelect() throws SQLException {
    try (Connection connection = connect()) {
      execute(connection, "CREATE TABLE test_table AS "
          + "SELECT c.id, c.name, p.population "
          + "FROM city c JOIN population p ON c.id = p.city_id");
      assertCityPopulation(connection, "test_table");
      try (Connection pg = dataSource().getConnection();
          Statement statement = pg.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_table")) {
        assertTrue(resultSet.next());
        assertEquals(2, resultSet.getInt(1));
      }
    }
  }

  @Test
  @Tag("integration")
  void createAndDropView() throws SQLException {
    try (Connection connection = connect()) {
      execute(connection, "CREATE VIEW city_view AS "
          + "SELECT c.id, c.name, p.population "
          + "FROM city c JOIN population p ON c.id = p.city_id");
      assertTrue(existsInPostgres(
          "SELECT 1 FROM information_schema.views WHERE table_name = 'city_view'"));
      assertCityPopulation(connection, "city_view");

      execute(connection, "DROP VIEW city_view");
      assertFalse(existsInPostgres(
          "SELECT 1 FROM information_schema.views WHERE table_name = 'city_view'"));
    }
  }
}
