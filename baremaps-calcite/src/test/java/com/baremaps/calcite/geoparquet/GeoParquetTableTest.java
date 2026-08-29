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

package com.baremaps.calcite.geoparquet;

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.testing.TestFiles;
import java.io.File;
import java.sql.*;
import java.util.Properties;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

/**
 * Tests for the GeoParquetTable class, which reads GeoParquet files and exposes them as Calcite
 * tables for SQL querying.
 */
class GeoParquetTableTest {

  private static final File SAMPLE_GEOPARQUET = TestFiles.GEOPARQUET.toFile();
  private GeoParquetTable table;
  private RelDataTypeFactory typeFactory;

  @BeforeEach
  void setUp() throws Exception {
    typeFactory = new JavaTypeFactoryImpl();
    table = new GeoParquetTable(SAMPLE_GEOPARQUET, typeFactory);
  }

  @Nested
  class SchemaTests {

    @Test
    void rowTypeContainsExpectedColumns() throws Exception {
      RelDataType rowType = table.getRowType(typeFactory);

      assertNotNull(rowType, "Row type should not be null");
      assertFalse(rowType.getFieldList().isEmpty(), "Row type should have fields");

      // Verify the schema contains a geometry column
      boolean hasGeometryColumn = rowType.getFieldNames().contains("geometry");
      assertTrue(hasGeometryColumn, "Schema should contain a geometry column");

      // Find geometry field and verify it's the correct type
      rowType.getFieldList().stream()
          .filter(field -> field.getName().equals("geometry"))
          .findFirst()
          .ifPresent(field -> {
            assertEquals(SqlTypeName.GEOMETRY, field.getType().getSqlTypeName(),
                "Geometry field should have the correct type");
          });
    }

    @Test
    void rowTypeFieldsAreCorrect() throws Exception {
      RelDataType rowType = table.getRowType(typeFactory);

      assertNotNull(rowType, "Row type should not be null");
      assertTrue(rowType.getFieldCount() > 0, "Row type should have fields");

      // Verify field names in row type
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
      calciteConnection.getRootSchema().add("GEOPARQUET", table);
    }

    @Test
    void simpleSelectReturnsExpectedResults() throws Exception {
      try (Statement statement = calciteConnection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT * FROM GEOPARQUET LIMIT 10")) {

        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        assertTrue(columnCount > 0, "Result should have columns");
        assertTrue(resultSet.next(), "Result should have at least one row");

        // Find and verify geometry column
        int geometryColumnIndex = findColumnIndex(metaData, "geometry");
        assertTrue(geometryColumnIndex > 0, "Should find geometry column");

        Object geometryValue = resultSet.getObject(geometryColumnIndex);
        assertNotNull(geometryValue, "Geometry column should have value");

        // Verify geometry is either a String WKT or a JTS Geometry
        assertValidGeometry(geometryValue);
      }
    }

    @Test
    void geometryFilterReturnsFilteredResults() throws Exception {
      try (Statement statement = calciteConnection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT * FROM GEOPARQUET WHERE geometry IS NOT NULL LIMIT 5")) {

        int rowCount = 0;
        while (resultSet.next()) {
          rowCount++;
          Object geometry = resultSet.getObject("geometry");
          assertNotNull(geometry, "Geometry should not be null");

          // Verify geometry is either a String WKT or a JTS Geometry
          assertValidGeometry(geometry);
        }

        assertTrue(rowCount > 0, "Should return at least one row");
        assertTrue(rowCount <= 5, "Should respect LIMIT clause");
      }
    }

    @Test
    void projectionLimitsColumns() throws Exception {
      try (Statement statement = calciteConnection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT geometry FROM GEOPARQUET LIMIT 5")) {

        ResultSetMetaData metaData = resultSet.getMetaData();
        assertEquals(1, metaData.getColumnCount(), "Should return only one column");
        assertEquals("geometry", metaData.getColumnName(1), "Column should be 'geometry'");

        int rowCount = 0;
        while (resultSet.next()) {
          rowCount++;
          Object geometry = resultSet.getObject(1);
          assertNotNull(geometry, "Geometry should not be null");

          // Verify geometry is either a String WKT or a JTS Geometry
          assertValidGeometry(geometry);
        }

        assertTrue(rowCount > 0, "Should return at least one row");
        assertTrue(rowCount <= 5, "Should respect LIMIT clause");
      }
    }

    /**
     * Helper method to check if an object is a valid geometry representation. Accepts either a WKT
     * string or a JTS Geometry object.
     */
    private void assertValidGeometry(Object geometry) {
      if (geometry instanceof String) {
        String wkt = (String) geometry;
        // Check if it contains typical WKT keywords
        assertTrue(
            wkt.contains("POLYGON") || wkt.contains("POINT") ||
                wkt.contains("LINESTRING") || wkt.contains("MULTIPOLYGON") ||
                wkt.contains("MULTIPOINT") || wkt.contains("MULTILINESTRING"),
            "Geometry column should contain valid WKT");
      } else if (geometry instanceof Geometry) {
        // JTS Geometry object is valid as is
        assertFalse(((Geometry) geometry).isEmpty(), "Geometry should not be empty");
      } else if (geometry instanceof byte[]) {
        // Binary geometry representation
        assertTrue(((byte[]) geometry).length > 0, "Binary geometry should not be empty");
      } else {
        fail("Unexpected geometry type: " + geometry.getClass().getName());
      }
    }

    private int findColumnIndex(ResultSetMetaData metaData, String columnName) throws SQLException {
      for (int i = 1; i <= metaData.getColumnCount(); i++) {
        if (metaData.getColumnName(i).equals(columnName)) {
          return i;
        }
      }
      return -1;
    }
  }
}
