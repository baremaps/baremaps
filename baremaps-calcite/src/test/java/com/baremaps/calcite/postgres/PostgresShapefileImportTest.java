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

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.calcite.shapefile.ShapefileTable;
import com.baremaps.testing.PostgresContainerTest;
import com.baremaps.testing.TestFiles;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Copies a shapefile into PostgreSQL with {@code CREATE TABLE ... AS SELECT}. */
class PostgresShapefileImportTest extends PostgresContainerTest {

  private static final String IMPORTED_TABLE = "imported_shapefile";

  @BeforeEach
  void setUp() throws SQLException {
    try (Connection connection = dataSource().getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE EXTENSION IF NOT EXISTS postgis");
      statement.execute("DROP TABLE IF EXISTS " + IMPORTED_TABLE + " CASCADE");
    }
  }

  private boolean existsInPostgres(String query) throws SQLException {
    try (Connection connection = dataSource().getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT EXISTS (" + query + ")")) {
      return resultSet.next() && resultSet.getBoolean(1);
    }
  }

  @Test
  @Tag("integration")
  void importShapefileToPostgres() throws Exception {
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");
    info.setProperty("caseSensitive", "false");
    info.setProperty("unquotedCasing", "TO_LOWER");
    info.setProperty("quotedCasing", "TO_LOWER");
    info.setProperty("parserFactory", PostgresDdlExecutor.class.getName() + "#PARSER_FACTORY");
    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();
      rootSchema.add("shapefile_data", new ShapefileTable(TestFiles.POINT_SHP.toFile()));
      rootSchema.add("pg",
          new PostgresSchema(dataSource(), "public", calciteConnection.getTypeFactory()));

      // The target schema is qualified; the source is resolved in the root schema
      try (Statement statement = connection.createStatement()) {
        statement.execute(
            "CREATE TABLE pg." + IMPORTED_TABLE + " AS SELECT * FROM shapefile_data");
      }

      assertTrue(existsInPostgres("SELECT 1 FROM information_schema.tables "
          + "WHERE table_name = '" + IMPORTED_TABLE + "'"));
      assertTrue(existsInPostgres("SELECT 1 FROM information_schema.columns "
          + "WHERE table_name = '" + IMPORTED_TABLE + "' "
          + "AND data_type = 'USER-DEFINED' AND udt_name = 'geometry'"),
          "Table should have a geometry column");
      assertTrue(existsInPostgres("SELECT 1 FROM " + IMPORTED_TABLE), "Table should have data");

      try (Statement statement = connection.createStatement();
          ResultSet resultSet =
              statement.executeQuery("SELECT * FROM pg." + IMPORTED_TABLE + " LIMIT 5")) {
        assertTrue(resultSet.next(), "Should have at least one row");
      }
    }
  }
}
