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

package com.baremaps.calcite.flatgeobuf;

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.testing.TestFiles;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the FlatGeoBufSchema class, which provides access to FlatGeoBuf files through the
 * Apache Calcite framework for SQL querying.
 */
class FlatGeoBufSchemaTest {

  private static final File SAMPLE_FLATGEOBUF_DIR = TestFiles.SAMPLE_FLATGEOBUF_DIR.toFile();

  @TempDir
  Path tempDir;

  @Test
  void testSchemaCreation() throws IOException {
    // Create a FlatGeoBufSchema instance
    FlatGeoBufSchema schema = new FlatGeoBufSchema(SAMPLE_FLATGEOBUF_DIR);

    // Get the table map
    Map<String, Table> tableMap = schema.getTableMap();

    // Verify that the schema has tables
    assertNotNull(tableMap);
    assertFalse(tableMap.isEmpty(), "Schema should have at least one table");

    // Verify that the test table exists
    assertTrue(tableMap.containsKey("countries"), "Schema should contain the 'countries' table");
    assertNotNull(tableMap.get("countries"), "Countries table should not be null");
  }

  @Test
  void testSqlQueryWithSchema() throws Exception {
    // Create a FlatGeoBufSchema instance
    FlatGeoBufSchema schema = new FlatGeoBufSchema(SAMPLE_FLATGEOBUF_DIR);

    // Configure Calcite connection properties
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");

    // Set up a connection and register our schema
    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Register the schema
      rootSchema.add("flatgeobuf", schema);

      // Execute a simple query
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT * FROM flatgeobuf.countries LIMIT 5")) {

        // Verify that we get results
        assertTrue(resultSet.next(), "Should have at least one row");

        // Verify that the result has a geometry column
        assertNotNull(resultSet.getObject("geometry"), "Geometry column should not be null");
      }
    }
  }

  @Test
  void testSchemaWithMultipleFiles() throws IOException {
    // Create a FlatGeoBufSchema instance with the temporary directory
    FlatGeoBufSchema schema = new FlatGeoBufSchema(tempDir.toFile());

    // Get the table map
    Map<String, Table> tableMap = schema.getTableMap();

    // Verify that the schema is empty (no .fgb files in the temp directory)
    assertTrue(tableMap.isEmpty(), "Schema should be empty for a directory with no .fgb files");
  }

  @Test
  void testSchemaWithNonExistentDirectory() throws IOException {
    // Create a non-existent directory path
    File nonExistentDir = tempDir.resolve("non-existent-directory").toFile();

    // Create a FlatGeoBufSchema instance with the non-existent directory
    FlatGeoBufSchema schema = new FlatGeoBufSchema(nonExistentDir);

    // Get the table map
    Map<String, Table> tableMap = schema.getTableMap();

    // Verify that the schema is empty
    assertTrue(tableMap.isEmpty(), "Schema should be empty for a non-existent directory");
  }

  @Test
  void testSchemaWithMultipleFlatGeoBufFiles() throws Exception {
    // Copy the sample FlatGeoBuf file to the temporary directory with different names
    File pointFile = TestFiles.POINT_FLATGEOBUF.toFile();

    // Copy the file with different names to simulate multiple files
    Files.copy(pointFile.toPath(), new File(tempDir.toFile(), "points.fgb").toPath());
    Files.copy(pointFile.toPath(), new File(tempDir.toFile(), "cities.fgb").toPath());

    // Create a FlatGeoBufSchema instance with the temporary directory
    FlatGeoBufSchema schema = new FlatGeoBufSchema(tempDir.toFile());

    // Get the table map
    Map<String, Table> tableMap = schema.getTableMap();

    // Verify that the schema has the expected tables
    assertEquals(2, tableMap.size(), "Schema should have 2 tables");
    assertTrue(tableMap.containsKey("points"), "Schema should contain the 'points' table");
    assertTrue(tableMap.containsKey("cities"), "Schema should contain the 'cities' table");

    // Test SQL query with one of the tables
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");

    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Register the schema
      rootSchema.add("multi_flatgeobuf", schema);

      // Execute a query on one of the tables
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT * FROM multi_flatgeobuf.points LIMIT 5")) {

        // Verify that we get results
        assertTrue(resultSet.next(), "Should have at least one row");
      }
    }
  }
}
