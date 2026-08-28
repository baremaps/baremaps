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
import java.util.Properties;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

class GeoPackageTableTest {

  private static final File SAMPLE_GEOPACKAGE = TestFiles.GEOPACKAGE.toFile();
  private static final String TABLE_NAME = "countries";

  @Test
  void testSchemaVerification() throws IOException {
    // Create a GeoPackageTable instance
    GeoPackageTable geoPackageTable = new GeoPackageTable(SAMPLE_GEOPACKAGE, TABLE_NAME);

    // Get the schema
    RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
    RelDataType rowType = geoPackageTable.getRowType(typeFactory);

    // Verify that the schema has the expected columns
    assertNotNull(rowType);
    assertTrue(rowType.getFieldCount() > 0);

    // Verify that there is a geometry column
    boolean hasGeometryColumn = false;
    for (int i = 0; i < rowType.getFieldCount(); i++) {
      RelDataType fieldType = rowType.getFieldList().get(i).getType();
      String columnName = rowType.getFieldList().get(i).getName();
      if (columnName.equalsIgnoreCase("geom") &&
          (fieldType.getSqlTypeName().getName().equals("GEOMETRY") ||
              fieldType.getSqlTypeName().getName().equals("JAVA_OBJECT"))
          &&
          fieldType.getFullTypeString().contains("org.locationtech.jts.geom.Geometry")) {
        hasGeometryColumn = true;
        break;
      }
    }
    assertTrue(hasGeometryColumn, "Schema should have a geometry column");
  }

  @Test
  void testSqlQueryWithRealGeoPackage() throws Exception {
    // Create a GeoPackageTable instance
    GeoPackageTable geoPackageTable = new GeoPackageTable(SAMPLE_GEOPACKAGE, TABLE_NAME);

    // Configure Calcite connection properties
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");

    // Set up a connection and register our table
    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Register the table in the root schema
      rootSchema.add(TABLE_NAME, geoPackageTable);

      // Execute a simple query
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT * FROM " + TABLE_NAME + " LIMIT 5")) {

        // Verify that we get results
        assertTrue(resultSet.next(), "Should have at least one row");

        // Verify that we can read the geometry column
        Object geomValue = resultSet.getObject("geom");
        assertNotNull(geomValue, "Geometry column should not be null");
      }
    }
  }

  @Test
  void testGeometryConversion() throws IOException {
    // Create a GeoPackageTable instance
    GeoPackageTable geoPackageTable = new GeoPackageTable(SAMPLE_GEOPACKAGE, TABLE_NAME);

    // Configure Calcite connection properties
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");

    // Set up a connection and register our table
    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Register the table in the root schema
      rootSchema.add(TABLE_NAME, geoPackageTable);

      // Execute a query to get the geometry
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT geom FROM " + TABLE_NAME + " LIMIT 1")) {

        assertTrue(resultSet.next(), "Should have at least one row");

        // Get the geometry value
        Object geomValue = resultSet.getObject("geom");
        assertNotNull(geomValue, "Geometry should not be null");
        assertTrue(geomValue instanceof Geometry, "Geometry should be a JTS Geometry");
      } catch (Exception e) {
        fail("Failed to execute query: " + e.getMessage());
      }
    } catch (Exception e) {
      fail("Failed to set up connection: " + e.getMessage());
    }
  }
}
