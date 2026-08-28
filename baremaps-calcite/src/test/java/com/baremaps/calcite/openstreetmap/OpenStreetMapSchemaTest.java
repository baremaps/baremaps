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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class OpenStreetMapSchemaTest {

  @TempDir
  Path tempDir;

  private Path sampleDataDir;
  private RelDataTypeFactory typeFactory;

  @BeforeEach
  void setup() throws IOException, SQLException {
    // Create a temporary directory for test files
    sampleDataDir = tempDir.resolve("osm-data");
    Files.createDirectories(sampleDataDir);

    // Get the absolute paths to the sample files
    Path pbfSourcePath = TestFiles.SAMPLE_OSM_PBF.toAbsolutePath();
    Path xmlSourcePath = TestFiles.SAMPLE_OSM_XML.toAbsolutePath();

    // Copy sample OSM files to the test directory
    Path pbfPath = sampleDataDir.resolve("sample.osm.pbf");
    Path xmlPath = sampleDataDir.resolve("sample.osm.xml");

    // Check if source files exist
    if (!Files.exists(pbfSourcePath)) {
      throw new IOException("Sample PBF file not found: " + pbfSourcePath);
    }
    if (!Files.exists(xmlSourcePath)) {
      throw new IOException("Sample XML file not found: " + xmlSourcePath);
    }

    Files.copy(pbfSourcePath, pbfPath);
    Files.copy(xmlSourcePath, xmlPath);

    // Set up Calcite connection to get a RelDataTypeFactory
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");

    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      typeFactory = calciteConnection.getTypeFactory();
    }
  }

  @Test
  void testSchemaCreation() throws IOException {
    // Create an OpenStreetMapSchema with the test directory
    OpenStreetMapSchema schema = new OpenStreetMapSchema(sampleDataDir.toFile(), typeFactory);

    // Verify that the schema contains the expected tables
    // The table name is based on the filename without extension
    assertTrue(schema.getTableMap().containsKey("sample"), "Schema should contain 'sample' table");

    // Verify that the table has the expected structure
    OpenStreetMapTable table = (OpenStreetMapTable) schema.getTableMap().get("sample");
    assertNotNull(table, "Table should not be null");

    // Verify the schema structure
    int fieldCount = table.getRowType(typeFactory).getFieldCount();
    assertEquals(9, fieldCount, "Schema should have 9 columns");
  }

  @Test
  void testSqlQueryWithDirectory() throws Exception {
    // Create an OpenStreetMapSchema with the test directory
    OpenStreetMapSchema schema = new OpenStreetMapSchema(sampleDataDir.toFile(), typeFactory);

    // Configure Calcite connection properties
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");

    // Set up a connection and register our schema
    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Add the schema to the root schema
      rootSchema.add("osm", schema);

      // Test a simple query to select a limited number of entities
      // The table name is based on the filename without extension
      try (Statement statement = connection.createStatement();
          ResultSet resultSet =
              statement.executeQuery("SELECT id, type FROM osm.sample LIMIT 10")) {
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

  @Test
  void testSqlQueryWithSingleFile() throws Exception {
    // Create a properly configured PbfEntityReader
    PbfEntityReader entityReader = new PbfEntityReader();
    entityReader.setGeometries(true);
    entityReader.setCoordinateMap(new HashMap<>());
    entityReader.setReferenceMap(new HashMap<>());

    // Create an OpenStreetMapSchema with a single file
    File pbfFile = sampleDataDir.resolve("sample.osm.pbf").toFile();
    OpenStreetMapSchema schema = new OpenStreetMapSchema(pbfFile, typeFactory, false);

    // Configure Calcite connection properties
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");

    // Set up a connection and register our schema
    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Add the schema to the root schema
      rootSchema.add("osm", schema);

      // Test a simple query to select a limited number of entities
      // For a single file, the table name is "sample" (not "osm")
      try (Statement statement = connection.createStatement();
          ResultSet resultSet =
              statement.executeQuery("SELECT id, type FROM osm.sample LIMIT 10")) {
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
