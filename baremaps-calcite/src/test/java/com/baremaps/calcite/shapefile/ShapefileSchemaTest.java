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

package com.baremaps.calcite.shapefile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.testing.TestFiles;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test class for the ShapefileSchema implementation.
 */
public class ShapefileSchemaTest {

  @TempDir
  Path tempDir;

  private Connection connection;
  private ShapefileSchema schema;

  @BeforeEach
  public void setup() throws SQLException, IOException {
    // Create a temporary directory for test files
    Path testDir = tempDir.resolve("shapefile-test");
    Files.createDirectories(testDir);

    // Copy the sample shapefile to the test directory
    Path sourceFile = TestFiles.POINT_SHP;
    Path targetFile = testDir.resolve("point.shp");
    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

    // Copy the associated .dbf file
    Path sourceDbf = sourceFile.resolveSibling("point.dbf");
    Path targetDbf = targetFile.resolveSibling("point.dbf");
    Files.copy(sourceDbf, targetDbf, StandardCopyOption.REPLACE_EXISTING);

    // Copy the associated .shx file
    Path sourceShx = sourceFile.resolveSibling("point.shx");
    Path targetShx = targetFile.resolveSibling("point.shx");
    Files.copy(sourceShx, targetShx, StandardCopyOption.REPLACE_EXISTING);
  }

  @AfterEach
  public void cleanup() throws SQLException {
    if (connection != null && !connection.isClosed()) {
      connection.close();
      connection = null;
    }

    // Force garbage collection to release file handles
    System.gc();
  }

  @Test
  public void testSchemaCreation() throws SQLException, IOException {
    // Create a connection to Calcite
    connection = DriverManager.getConnection("jdbc:calcite:");
    CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
    SchemaPlus rootSchema = calciteConnection.getRootSchema();

    // Create and register the shapefile schema
    schema = new ShapefileSchema(tempDir.resolve("shapefile-test").toFile(),
        calciteConnection.getTypeFactory());
    rootSchema.add("shapefile", schema);

    // Verify that the schema contains the expected table
    assertTrue(rootSchema.getSubSchemaNames().contains("shapefile"));
    assertTrue(rootSchema.getSubSchema("shapefile").getTableNames().contains("point"));
  }

  @Test
  public void testSqlQuery() throws SQLException, IOException {
    // Create a connection to Calcite
    connection = DriverManager.getConnection("jdbc:calcite:");
    CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
    SchemaPlus rootSchema = calciteConnection.getRootSchema();

    // Create and register the shapefile schema
    schema = new ShapefileSchema(tempDir.resolve("shapefile-test").toFile(),
        calciteConnection.getTypeFactory());
    rootSchema.add("shapefile", schema);

    // Execute a simple SQL query - use lowercase for schema and table names
    Statement statement = connection.createStatement();
    ResultSet resultSet = statement.executeQuery(
        "SELECT * FROM \"shapefile\".\"point\"");

    // Verify that we get results
    assertTrue(resultSet.next());

    // Verify that the result set has the expected columns
    // Note: The actual column names will depend on the sample shapefile
    assertNotNull(resultSet.getMetaData());
    assertTrue(resultSet.getMetaData().getColumnCount() > 0);

    // Close resources
    resultSet.close();
    statement.close();
  }

  @Test
  public void testSingleFileSchema() throws SQLException, IOException {
    // Create a connection to Calcite
    connection = DriverManager.getConnection("jdbc:calcite:");
    CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
    SchemaPlus rootSchema = calciteConnection.getRootSchema();

    // Get the sample shapefile
    File sampleFile = tempDir.resolve("shapefile-test").resolve("point.shp").toFile();

    // Create and register the shapefile schema with a single file
    schema =
        new ShapefileSchema(sampleFile, calciteConnection.getTypeFactory(), false);
    rootSchema.add("single", schema);

    // Verify that the schema contains the expected table
    assertTrue(rootSchema.getSubSchemaNames().contains("single"));
    assertTrue(rootSchema.getSubSchema("single").getTableNames().contains("point"));

    // Execute a simple SQL query - use lowercase for schema and table names
    Statement statement = connection.createStatement();
    ResultSet resultSet = statement.executeQuery(
        "SELECT * FROM \"single\".\"point\"");

    // Verify that we get results
    assertTrue(resultSet.next());

    // Verify that the result set has the expected columns
    assertNotNull(resultSet.getMetaData());
    assertTrue(resultSet.getMetaData().getColumnCount() > 0);

    // Close resources
    resultSet.close();
    statement.close();
  }
}
