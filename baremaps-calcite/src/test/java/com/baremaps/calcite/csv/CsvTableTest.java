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

package com.baremaps.calcite.csv;

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.testing.TestFiles;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

public class CsvTableTest {

  private static final File CITIES_CSV = TestFiles.CITIES_CSV.toFile();
  private static final char SEPARATOR = ',';
  private static final boolean HAS_HEADER = true;

  @Test
  void testSchemaVerification() throws IOException {
    // Create a CsvTable for cities.csv
    CsvTable csvTable = new CsvTable(CITIES_CSV, SEPARATOR, HAS_HEADER);

    // Verify the schema structure
    RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
    RelDataType rowType = csvTable.getRowType(typeFactory);

    // Get field count
    int fieldCount = rowType.getFieldCount();

    // Verify the schema has the expected number of columns
    assertEquals(3, fieldCount, "Schema should have 3 columns");

    // Verify column names
    String[] expectedColumnNames = {"city", "country", "population"};

    for (int i = 0; i < fieldCount; i++) {
      assertEquals(expectedColumnNames[i], rowType.getFieldList().get(i).getName(),
          "Column " + i + " should have name " + expectedColumnNames[i]);
    }
  }

  @Test
  void testSqlQueryWithRealCsvFile() throws Exception {
    // Create the table
    CsvTable csvTable = new CsvTable(CITIES_CSV, SEPARATOR, HAS_HEADER);

    // Configure Calcite connection properties
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");

    // Set up a connection and register our table
    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Add the table to the schema
      rootSchema.add("cities", csvTable);

      // Test a simple query to select all rows
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT * FROM cities")) {
        int rowCount = 0;

        while (resultSet.next()) {
          rowCount++;
          String city = resultSet.getString("city");
          String country = resultSet.getString("country");
          String population = resultSet.getString("population");

          // Verify basic properties
          assertNotNull(city, "Row should have a city");
          assertNotNull(country, "Row should have a country");
          assertNotNull(population, "Row should have a population");
        }

        // Verify that we got the expected number of rows
        assertEquals(3, rowCount, "Should have retrieved 3 rows");
      }

      // Test a query with a filter
      try (Statement statement = connection.createStatement();
          ResultSet resultSet =
              statement.executeQuery("SELECT * FROM cities WHERE country = 'France'")) {
        int rowCount = 0;

        while (resultSet.next()) {
          rowCount++;
          String city = resultSet.getString("city");
          String country = resultSet.getString("country");
          String population = resultSet.getString("population");

          // Verify that this is the row we expect
          assertEquals("Paris", city, "City should be Paris");
          assertEquals("France", country, "Country should be France");
          assertEquals("2148000", population, "Population should be 2148000");
        }

        // Verify that we got exactly one row
        assertEquals(1, rowCount, "Should have retrieved exactly one row");
      }
    }
  }

  @Test
  void testCsvWithoutHeader() throws Exception {
    // Create a temporary file without a header
    File tempFile = File.createTempFile("no_header", ".csv");
    tempFile.deleteOnExit();

    // Write data without header
    Files.writeString(tempFile.toPath(),
        "Paris,France,2148000\n" +
            "London,UK,8982000\n" +
            "Tokyo,Japan,37400000\n");

    // Create a CsvTable with hasHeader=false
    CsvTable csvTable = new CsvTable(tempFile, SEPARATOR, false);

    // Verify the schema structure
    RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
    RelDataType rowType = csvTable.getRowType(typeFactory);

    // Get field count
    int fieldCount = rowType.getFieldCount();

    // Verify the schema has the expected number of columns
    assertEquals(3, fieldCount, "Schema should have 3 columns");

    // Verify column names (should be column1, column2, etc.)
    for (int i = 0; i < fieldCount; i++) {
      assertEquals("column" + (i + 1), rowType.getFieldList().get(i).getName(),
          "Column " + i + " should have name column" + (i + 1));
    }
  }

  @Test
  void testCsvWithCustomSeparator() throws Exception {
    // Create a temporary file with a custom separator
    File tempFile = File.createTempFile("custom_separator", ".csv");
    tempFile.deleteOnExit();

    // Write data with pipe separator
    Files.writeString(tempFile.toPath(),
        "city|country|population\n" +
            "Paris|France|2148000\n" +
            "London|UK|8982000\n" +
            "Tokyo|Japan|37400000\n");

    // Create a CsvTable with a custom separator
    CsvTable csvTable = new CsvTable(tempFile, '|', true);

    // Verify the schema structure
    RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
    RelDataType rowType = csvTable.getRowType(typeFactory);

    // Get field count
    int fieldCount = rowType.getFieldCount();

    // Verify the schema has the expected number of columns
    assertEquals(3, fieldCount, "Schema should have 3 columns");

    // Verify column names
    String[] expectedColumnNames = {"city", "country", "population"};

    for (int i = 0; i < fieldCount; i++) {
      assertEquals(expectedColumnNames[i], rowType.getFieldList().get(i).getName(),
          "Column " + i + " should have name " + expectedColumnNames[i]);
    }
  }
}
