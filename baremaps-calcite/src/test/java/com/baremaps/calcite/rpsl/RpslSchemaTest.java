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

package com.baremaps.calcite.rpsl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test class for the RpslSchema implementation.
 */
public class RpslSchemaTest {

  @TempDir
  Path tempDir;

  @BeforeEach
  public void setup() throws SQLException, IOException {
    // Create a temporary directory for test files
    Path testDir = tempDir.resolve("rpsl-test");
    Files.createDirectories(testDir);

    // Copy the sample RPSL file from TestFiles
    Path sourceFile = TestFiles.RPSL_TXT;
    Path targetFile = testDir.resolve("sample.txt");
    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
  }

  @Test
  public void testSchemaCreation() throws SQLException, IOException {
    // Create a connection to Calcite
    Connection connection = DriverManager.getConnection("jdbc:calcite:");
    CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
    SchemaPlus rootSchema = calciteConnection.getRootSchema();

    // Create and register the RPSL schema
    RpslSchema schema =
        new RpslSchema(tempDir.resolve("rpsl-test").toFile());
    rootSchema.add("rpsl", schema);

    // Verify that the schema contains the expected table
    assertTrue(rootSchema.getSubSchemaNames().contains("rpsl"));
    assertTrue(rootSchema.getSubSchema("rpsl").getTableNames().contains("sample"));

    connection.close();
  }

  @Test
  public void testSqlQuery() throws SQLException, IOException {
    // Create a connection to Calcite
    Connection connection = DriverManager.getConnection("jdbc:calcite:");
    CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
    SchemaPlus rootSchema = calciteConnection.getRootSchema();

    // Create and register the RPSL schema
    RpslSchema schema =
        new RpslSchema(tempDir.resolve("rpsl-test").toFile());
    rootSchema.add("rpsl", schema);

    // Execute a simple SQL query - use lowercase for schema and table names
    try (Statement statement = connection.createStatement()) {
      ResultSet resultSet = statement.executeQuery(
          "SELECT * FROM \"rpsl\".\"sample\"");

      // Verify that we get results
      assertTrue(resultSet.next());

      // Verify that the result set has the expected columns
      assertNotNull(resultSet.getMetaData());
      assertTrue(resultSet.getMetaData().getColumnCount() > 0);

      // Verify that we can access the data
      String inetnum = resultSet.getString("inetnum");
      assertNotNull(inetnum);
    }

    connection.close();
  }

  @Test
  public void testSingleFileSchema() throws SQLException, IOException {
    // Create a connection to Calcite
    Connection connection = DriverManager.getConnection("jdbc:calcite:");
    CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
    SchemaPlus rootSchema = calciteConnection.getRootSchema();

    // Get the sample RPSL file
    File sampleFile = tempDir.resolve("rpsl-test").resolve("sample.txt").toFile();

    // Create and register the RPSL schema with a single file
    RpslSchema schema = new RpslSchema(sampleFile);
    rootSchema.add("single", schema);

    // Verify that the schema contains the expected table
    assertTrue(rootSchema.getSubSchemaNames().contains("single"));
    assertTrue(rootSchema.getSubSchema("single").getTableNames().contains("sample"));

    // Execute a simple SQL query - use lowercase for schema and table names
    try (Statement statement = connection.createStatement()) {
      ResultSet resultSet = statement.executeQuery(
          "SELECT * FROM \"single\".\"sample\"");

      // Verify that we get results
      assertTrue(resultSet.next());

      // Verify that the result set has the expected columns
      assertNotNull(resultSet.getMetaData());
      assertTrue(resultSet.getMetaData().getColumnCount() > 0);

      // Verify that we can access the data
      String inetnum = resultSet.getString("inetnum");
      assertNotNull(inetnum);
    }

    connection.close();
  }
}
