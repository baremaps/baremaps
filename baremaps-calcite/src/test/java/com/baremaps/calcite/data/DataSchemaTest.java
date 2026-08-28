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

package com.baremaps.calcite.data;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.config.CalciteConnectionConfigImpl;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataSchemaTest {

  @TempDir
  Path tempDir;

  private File sampleDataDir;
  private File citiesDir;
  private File countriesDir;
  private RelDataTypeFactory typeFactory;
  private Connection connection;
  private DataSchema schema;

  @BeforeEach
  void setup() throws IOException, SQLException {
    // Create the test directory structure
    sampleDataDir = tempDir.resolve("data").toFile();
    citiesDir = new File(sampleDataDir, "cities");
    countriesDir = new File(sampleDataDir, "countries");

    sampleDataDir.mkdirs();
    citiesDir.mkdirs();
    countriesDir.mkdirs();

    // Create schema files
    createCitiesSchema();
    createCountriesSchema();

    // Initialize the type factory
    Properties props = new Properties();
    props.setProperty(CalciteConnectionProperty.CASE_SENSITIVE.camelName(), "false");
    CalciteConnectionConfig config = new CalciteConnectionConfigImpl(props);

    // Create a connection to get the type factory
    connection = DriverManager.getConnection("jdbc:calcite:", props);
    CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
    typeFactory = calciteConnection.getTypeFactory();
  }

  @AfterEach
  void cleanup() throws SQLException {
    if (connection != null && !connection.isClosed()) {
      connection.close();
      connection = null;
    }

    // Force garbage collection to release file handles
    System.gc();
  }

  private void createCitiesSchema() throws IOException {
    // Create a schema for cities
    Map<String, Object> schemaMap = new HashMap<>();
    schemaMap.put("name", "cities");

    // Define columns
    Map<String, Object>[] columns = new Map[3];

    // city column
    Map<String, Object> cityColumn = new HashMap<>();
    cityColumn.put("name", "city");
    cityColumn.put("cardinality", "REQUIRED");
    cityColumn.put("sqlTypeName", "VARCHAR");
    columns[0] = cityColumn;

    // country column
    Map<String, Object> countryColumn = new HashMap<>();
    countryColumn.put("name", "country");
    countryColumn.put("cardinality", "REQUIRED");
    countryColumn.put("sqlTypeName", "VARCHAR");
    columns[1] = countryColumn;

    // population column
    Map<String, Object> populationColumn = new HashMap<>();
    populationColumn.put("name", "population");
    populationColumn.put("cardinality", "REQUIRED");
    populationColumn.put("sqlTypeName", "INTEGER");
    columns[2] = populationColumn;

    schemaMap.put("columns", columns);

    // Write schema to file
    ObjectMapper mapper = new ObjectMapper();
    try (FileOutputStream fos = new FileOutputStream(new File(citiesDir, "schema.json"))) {
      mapper.writeValue(fos, schemaMap);
    }
  }

  private void createCountriesSchema() throws IOException {
    // Create a schema for countries
    Map<String, Object> schemaMap = new HashMap<>();
    schemaMap.put("name", "countries");

    // Define columns
    Map<String, Object>[] columns = new Map[3];

    // country column
    Map<String, Object> countryColumn = new HashMap<>();
    countryColumn.put("name", "country");
    countryColumn.put("cardinality", "REQUIRED");
    countryColumn.put("sqlTypeName", "VARCHAR");
    columns[0] = countryColumn;

    // continent column
    Map<String, Object> continentColumn = new HashMap<>();
    continentColumn.put("name", "continent");
    continentColumn.put("cardinality", "REQUIRED");
    continentColumn.put("sqlTypeName", "VARCHAR");
    columns[1] = continentColumn;

    // population column
    Map<String, Object> populationColumn = new HashMap<>();
    populationColumn.put("name", "population");
    populationColumn.put("cardinality", "REQUIRED");
    populationColumn.put("sqlTypeName", "INTEGER");
    columns[2] = populationColumn;

    schemaMap.put("columns", columns);

    // Write schema to file
    ObjectMapper mapper = new ObjectMapper();
    try (FileOutputStream fos = new FileOutputStream(new File(countriesDir, "schema.json"))) {
      mapper.writeValue(fos, schemaMap);
    }
  }

  @Test
  @Tag("integration")
  void testSchemaCreation() throws IOException {
    // Create a DataSchema instance
    schema = new DataSchema(sampleDataDir, typeFactory);

    // Get the table map
    Map<String, Table> tableMap = schema.getTableMap();

    // Verify that the schema has tables
    assertNotNull(tableMap);
    assertFalse(tableMap.isEmpty(), "Schema should have at least one table");

    // Verify that both test tables exist
    assertTrue(tableMap.containsKey("cities"), "Schema should contain the 'cities' table");
    assertTrue(tableMap.containsKey("countries"), "Schema should contain the 'countries' table");
    assertNotNull(tableMap.get("cities"), "Cities table should not be null");
    assertNotNull(tableMap.get("countries"), "Countries table should not be null");
  }

  @Test
  @Tag("integration")
  void testSqlQueryWithSchema() throws Exception {
    // Create a DataSchema instance
    schema = new DataSchema(sampleDataDir, typeFactory);

    // Configure Calcite connection properties
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");

    // Set up a connection and register our schema
    connection = DriverManager.getConnection("jdbc:calcite:", info);
    CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
    SchemaPlus rootSchema = calciteConnection.getRootSchema();

    // Register the schema
    rootSchema.add("data", schema);

    // Execute a simple query
    Statement statement = connection.createStatement();
    ResultSet resultSet = statement.executeQuery(
        "SELECT * FROM data.cities WHERE country = 'France'");

    // Since we don't have actual data in the tables, we just verify the query executes
    // In a real test, we would add data to the tables and verify the results
    assertNotNull(resultSet, "ResultSet should not be null");

    // Close resources
    resultSet.close();
    statement.close();
  }

  @Test
  @Tag("integration")
  void testJoinQuery() throws Exception {
    // Create a DataSchema instance
    schema = new DataSchema(sampleDataDir, typeFactory);

    // Configure Calcite connection properties
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");

    // Set up a connection and register our schema
    connection = DriverManager.getConnection("jdbc:calcite:", info);
    CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
    SchemaPlus rootSchema = calciteConnection.getRootSchema();

    // Register the schema
    rootSchema.add("data", schema);

    // Execute a join query
    Statement statement = connection.createStatement();
    ResultSet resultSet = statement.executeQuery(
        "SELECT c.city, c.country, co.continent "
            + "FROM data.cities c "
            + "JOIN data.countries co ON c.country = co.country "
            + "WHERE co.continent = 'Europe'");

    // Since we don't have actual data in the tables, we just verify the query executes
    // In a real test, we would add data to the tables and verify the results
    assertNotNull(resultSet, "ResultSet should not be null");

    // Close resources
    resultSet.close();
    statement.close();
  }
}
