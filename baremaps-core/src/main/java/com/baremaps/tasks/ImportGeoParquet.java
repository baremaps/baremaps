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

package com.baremaps.tasks;

import com.baremaps.calcite.postgres.PostgresDdlExecutor;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import com.baremaps.workflow.WorkflowException;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import a GeoParquet into a database using Calcite.
 */
public class ImportGeoParquet implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ImportGeoParquet.class);

  private URI uri;
  private String tableName;
  private Object database;
  private Integer databaseSrid;

  /**
   * Constructs a {@code ImportGeoParquet}.
   */
  public ImportGeoParquet() {

  }

  /**
   * Constructs an {@code ImportGeoParquet}.
   *
   * @param uri the GeoParquet uri
   * @param tableName the target table name
   * @param database the database
   * @param databaseSrid the target SRID
   */
  public ImportGeoParquet(URI uri, String tableName, Object database, Integer databaseSrid) {
    this.uri = uri;
    this.tableName = tableName;
    this.database = database;
    this.databaseSrid = databaseSrid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(WorkflowContext context) throws Exception {
    // Validate required parameters
    if (uri == null) {
      throw new WorkflowException("GeoParquet URI cannot be null");
    }
    if (tableName == null || tableName.isEmpty()) {
      throw new WorkflowException("Table name cannot be null or empty");
    }
    if (database == null) {
      throw new WorkflowException("Database connection cannot be null");
    }
    if (databaseSrid == null) {
      throw new WorkflowException("Target SRID cannot be null");
    }

    logger.info("Importing GeoParquet from: {}", uri);

    var dataSource = context.getDataSource(database);

    // Sanitize table name to prevent SQL injection
    String sanitizedTableName = sanitizeTableName(tableName);
    logger.info("Creating table: {}", sanitizedTableName);

    // Set ThreadLocal DataSource for PostgresDdlExecutor to use
    PostgresDdlExecutor.setThreadLocalDataSource(dataSource);

    try {
      // Setup Calcite connection properties
      Properties info = new Properties();
      info.setProperty("lex", "MYSQL");
      info.setProperty("caseSensitive", "false");
      info.setProperty("unquotedCasing", "TO_LOWER");
      info.setProperty("quotedCasing", "TO_LOWER");
      info.setProperty("parserFactory", PostgresDdlExecutor.class.getName() + "#PARSER_FACTORY");

      // Create a connection to Calcite
      try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {

        // Get the list of tables in the GeoParquet
        String[] tables = getGeoParquetTables(connection);

        if (tables.length == 0) {
          logger.warn("No tables found in GeoParquet: {}", uri);
          return;
        }

        // Import each table
        for (String sourceTableName : tables) {
          // Create a temporary table name for the GeoParquet data
          String tempTableName =
              "geoparquet_data_" + System.currentTimeMillis() + "_" + sourceTableName;

          // Register the GeoParquet table in the Calcite schema
          String registerSql = "CREATE TABLE " + tempTableName + " AS " +
              "SELECT * FROM " + sourceTableName;

          logger.info("Executing SQL: {}", registerSql);

          // Execute the DDL statement to create the table
          try (Statement statement = connection.createStatement()) {
            statement.execute(registerSql);
          }

          // Set SRID on geometry column if specified
          try (Connection pgConnection = dataSource.getConnection();
              Statement stmt = pgConnection.createStatement()) {
            stmt.execute(String.format(
                "SELECT UpdateGeometrySRID('%s', 'geometry', %d)",
                sanitizedTableName, databaseSrid));
          }

          // Verify that the table was created in PostgreSQL
          try (Connection pgConnection = dataSource.getConnection();
              Statement statement = pgConnection.createStatement();
              ResultSet resultSet = statement.executeQuery(
                  "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = '" +
                      sanitizedTableName + "')")) {
            if (!resultSet.next() || !resultSet.getBoolean(1)) {
              throw new WorkflowException("Failed to create table: " + sanitizedTableName);
            }
          }

          // Verify that the table has data
          try (Connection pgConnection = dataSource.getConnection();
              Statement statement = pgConnection.createStatement();
              ResultSet resultSet = statement.executeQuery(
                  "SELECT COUNT(*) FROM " + sanitizedTableName)) {
            if (resultSet.next()) {
              int count = resultSet.getInt(1);
              logger.info("Imported {} rows to table: {}", count, sanitizedTableName);
              if (count == 0) {
                logger.warn("No rows were imported from GeoParquet to table: {}",
                    sanitizedTableName);
              }
            }
          }
        }
      }
    } finally {
      // Clean up thread local storage
      PostgresDdlExecutor.clearThreadLocalDataSource();
    }

    logger.info("Successfully imported GeoParquet to table: {}", sanitizedTableName);
  }

  /**
   * Gets the list of tables in the GeoParquet.
   * 
   * @param connection the Calcite connection
   * @return the list of table names
   * @throws Exception if an error occurs
   */
  private String[] getGeoParquetTables(Connection connection) throws Exception {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SHOW TABLES")) {
      java.util.List<String> tables = new java.util.ArrayList<>();
      while (resultSet.next()) {
        tables.add(resultSet.getString(1));
      }
      return tables.toArray(new String[0]);
    }
  }

  /**
   * Sanitizes a table name to prevent SQL injection.
   * 
   * @param name the table name to sanitize
   * @return the sanitized table name
   */
  private String sanitizeTableName(String name) {
    // Replace any non-alphanumeric characters with underscores
    return name.replaceAll("[^a-zA-Z0-9]", "_");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new StringJoiner(", ", ImportGeoParquet.class.getSimpleName() + "[", "]")
        .add("uri=" + uri)
        .add("tableName=" + tableName)
        .add("database=" + database)
        .add("databaseSrid=" + databaseSrid)
        .toString();
  }
}
