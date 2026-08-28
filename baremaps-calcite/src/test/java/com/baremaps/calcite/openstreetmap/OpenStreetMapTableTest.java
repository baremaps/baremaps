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

package com.baremaps.calcite.openstreetmap;

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.openstreetmap.pbf.PbfEntityReader;
import com.baremaps.testing.TestFiles;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

public class OpenStreetMapTableTest {

  private static final Path SAMPLE_OSM_PATH =
      TestFiles.resolve("baremaps-testing/data/osm-sample/sample.osm.pbf");

  @Test
  void testSchemaVerification() throws Exception {
    try (var inputStream = Files.newInputStream(SAMPLE_OSM_PATH)) {
      // Create a PBF entity reader
      PbfEntityReader entityReader = new PbfEntityReader();

      // Create the OpenStreetMapTable
      OpenStreetMapTable osmTable = new OpenStreetMapTable(SAMPLE_OSM_PATH.toFile(), entityReader);

      // Verify the schema structure
      RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
      RelDataType rowType = osmTable.getRowType(typeFactory);

      // Get field count
      int fieldCount = rowType.getFieldCount();

      // Verify the schema has the expected number of columns
      assertEquals(9, fieldCount, "Schema should have 9 columns");

      // Verify column names
      String[] expectedColumnNames = {
          "id", "type", "version", "timestamp", "uid", "user", "changeset", "tags", "geometry"
      };

      for (int i = 0; i < fieldCount; i++) {
        assertEquals(expectedColumnNames[i], rowType.getFieldList().get(i).getName(),
            "Column " + i + " should have name " + expectedColumnNames[i]);
      }

      System.out.println("OpenStreetMapTable schema verified successfully");
    } catch (IOException e) {
      fail("Error reading test file: " + e.getMessage());
    }
  }

  @Test
  void testSqlQueryWithRealPbfFile() throws Exception {
    // Create a properly configured reader and table directly
    PbfEntityReader entityReader = new PbfEntityReader();

    try (var inputStream = new FileInputStream(SAMPLE_OSM_PATH.toFile())) {
      // Create the table with our configured reader
      OpenStreetMapTable osmTable = new OpenStreetMapTable(SAMPLE_OSM_PATH.toFile(), entityReader);

      // Configure Calcite connection properties
      Properties info = new Properties();
      info.setProperty("lex", "MYSQL");

      // Set up a connection and register our table
      try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
        CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
        SchemaPlus rootSchema = calciteConnection.getRootSchema();

        // Add the table to the schema
        rootSchema.add("osm", osmTable);

        // Test a simple query to select a limited number of entities
        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT id, type FROM osm LIMIT 10")) {
          int rowCount = 0;

          while (resultSet.next()) {
            rowCount++;
            long id = resultSet.getLong("id");
            String type = resultSet.getString("type");

            // Verify basic properties
            assertTrue(id != 0, "Entity should have non-zero ID");
            assertNotNull(type, "Entity should have a type");
          }

          // Verify that we got some rows
          assertTrue(rowCount > 0, "Should have retrieved at least one entity");
        }
      }
    }
  }
}
