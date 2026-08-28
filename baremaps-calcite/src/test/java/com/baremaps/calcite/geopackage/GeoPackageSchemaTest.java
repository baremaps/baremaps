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

package com.baremaps.calcite.geopackage;

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.testing.TestFiles;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.junit.jupiter.api.Test;

class GeoPackageSchemaTest {

  private static final File SAMPLE_GEOPACKAGE = TestFiles.GEOPACKAGE.toFile();

  @Test
  void testSchemaCreation() throws IOException {
    // Create a GeoPackageSchema instance
    GeoPackageSchema schema = new GeoPackageSchema(SAMPLE_GEOPACKAGE);

    // Get the table map
    Map<String, Table> tableMap = schema.getTableMap();

    // Verify that the schema has tables
    assertNotNull(tableMap);
    assertFalse(tableMap.isEmpty(), "Schema should have at least one table");

    // Verify that the 'countries' table exists
    assertTrue(tableMap.containsKey("countries"), "Schema should contain the 'countries' table");
    assertNotNull(tableMap.get("countries"), "Countries table should not be null");
  }

  @Test
  void testSqlQueryWithSchema() throws Exception {
    // Create a GeoPackageSchema instance
    GeoPackageSchema schema = new GeoPackageSchema(SAMPLE_GEOPACKAGE);

    // Configure Calcite connection properties
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");

    // Set up a connection and register our schema
    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Register the schema
      rootSchema.add("geopackage", schema);

      // Execute a simple query
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT * FROM geopackage.countries LIMIT 5")) {

        // Verify that we get results
        assertTrue(resultSet.next(), "Should have at least one row");

        // Verify that we can read the geometry column
        Object geomValue = resultSet.getObject("geom");
        assertNotNull(geomValue, "Geometry column should not be null");
      }
    }
  }

  @Test
  void testMultipleTableAccess() throws Exception {
    // Create a GeoPackageSchema instance
    GeoPackageSchema schema = new GeoPackageSchema(SAMPLE_GEOPACKAGE);

    // Get the table map
    Map<String, Table> tableMap = schema.getTableMap();

    // Configure Calcite connection properties
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");

    // Set up a connection and register our schema
    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Register the schema
      rootSchema.add("geopackage", schema);

      // Test each table in the schema
      for (String tableName : tableMap.keySet()) {
        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT * FROM geopackage." + tableName + " LIMIT 1")) {

          assertTrue(resultSet.next(), "Should have at least one row in table: " + tableName);
        }
      }
    }
  }
}
