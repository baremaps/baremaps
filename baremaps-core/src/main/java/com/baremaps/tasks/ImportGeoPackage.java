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

import com.baremaps.calcite.geopackage.GeoPackageSchema;
import com.baremaps.calcite.postgres.PostgresDdlExecutor;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import com.baremaps.workflow.WorkflowException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import a GeoPackage into a database using Calcite.
 */
public class ImportGeoPackage implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ImportGeoPackage.class);

  private Path file;
  private Integer fileSrid;
  private Object database;
  private Integer databaseSrid;

  /**
   * Constructs a {@code ImportGeoPackage}.
   */
  public ImportGeoPackage() {}

  /**
   * Constructs an {@code ImportGeoPackage}.
   *
   * @param file the GeoPackage file
   * @param fileSrid the source SRID
   * @param database the database
   * @param databaseSrid the target SRID
   */
  public ImportGeoPackage(Path file, Integer fileSrid, Object database, Integer databaseSrid) {
    this.file = file;
    this.fileSrid = fileSrid;
    this.database = database;
    this.databaseSrid = databaseSrid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(WorkflowContext context) throws Exception {
    // Validate required parameters
    if (file == null) {
      throw new WorkflowException("GeoPackage file path cannot be null");
    }
    if (fileSrid == null) {
      throw new WorkflowException("Source SRID cannot be null");
    }
    if (database == null) {
      throw new WorkflowException("Database connection cannot be null");
    }
    if (databaseSrid == null) {
      throw new WorkflowException("Target SRID cannot be null");
    }

    var path = file.toAbsolutePath();
    logger.info("Importing GeoPackage from: {}", path);

    var dataSource = context.getDataSource(database);

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

      // Create a GeoPackageSchema instance
      GeoPackageSchema geoPackageSchema = new GeoPackageSchema(path.toFile());

      // Create a temporary schema name for the GeoPackage data
      String schemaName = "geopackage_schema_" + System.currentTimeMillis();

      try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
        CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
        SchemaPlus rootSchema = calciteConnection.getRootSchema();

        // Register the GeoPackage schema in the Calcite schema
        rootSchema.add(schemaName, geoPackageSchema);

        // Debug logging to check schema registration
        Schema registeredSchema = rootSchema.getSubSchema(schemaName);
        logger.info("Registered schema class: {}",
            registeredSchema != null ? registeredSchema.getClass().getName() : "null");
        logger.info("Is GeoPackageSchema: {}", registeredSchema instanceof GeoPackageSchema);

        // Get the list of tables in the GeoPackage
        List<String> tables = new ArrayList<>();

        // Get the tables directly from the GeoPackage file
        GeoPackage geoPackage = GeoPackageManager.open(file.toFile());
        tables.addAll(geoPackage.getFeatureTables());
        geoPackage.close();

        if (tables.isEmpty()) {
          logger.warn("No tables found in GeoPackage: {}", path);
          return;
        }

        // Import each table
        for (String tableName : tables) {
          // Sanitize table name to prevent SQL injection
          String sanitizedTableName = sanitizeTableName(tableName);
          logger.info("Importing table: {} to: {}", tableName, sanitizedTableName);

          // Create a table in PostgreSQL by selecting from the GeoPackage table
          String createTableSql = "CREATE TABLE " + sanitizedTableName + " AS " +
              "SELECT * FROM " + schemaName + "." + tableName;

          logger.info("Executing SQL: {}", createTableSql);

          // Execute the DDL statement to create the table
          try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSql);
          }

          // Set SRID on geometry column if specified
          try (Connection pgConnection = dataSource.getConnection();
              Statement stmt = pgConnection.createStatement()) {
            stmt.execute(String.format(
                "SELECT UpdateGeometrySRID('%s', 'geom', %d)",
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
                logger.warn("No rows were imported from GeoPackage to table: {}",
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

    logger.info("Successfully imported GeoPackage to database");
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
    return new StringJoiner(", ", ImportGeoPackage.class.getSimpleName() + "[", "]")
        .add("file=" + file)
        .add("fileSrid=" + fileSrid)
        .add("database=" + database)
        .add("databaseSrid=" + databaseSrid)
        .toString();
  }
}
