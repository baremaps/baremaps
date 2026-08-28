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
import com.baremaps.calcite.shapefile.ShapefileTable;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import com.baremaps.workflow.WorkflowException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.StringJoiner;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import a shapefile into a database using Calcite.
 */
public class ImportShapefile implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ImportShapefile.class);

  private Path file;
  private Integer fileSrid;
  private Object database;
  private Integer databaseSrid;

  /**
   * Constructs a {@code ImportShapefile}.
   */
  public ImportShapefile() {

  }

  /**
   * Constructs an {@code ImportShapefile}.
   *
   * @param file the shapefile file
   * @param fileSrid the source SRID
   * @param database the database
   * @param databaseSrid the target SRID
   */
  public ImportShapefile(Path file, Integer fileSrid, Object database, Integer databaseSrid) {
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
      throw new WorkflowException("Shapefile path cannot be null");
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
    logger.info("Importing shapefile from: {}", path);

    var dataSource = context.getDataSource(database);
    // Sanitize table name to prevent SQL injection
    var tableName = sanitizeTableName(
        file.getFileName().toString().replaceFirst("[.][^.]+$", "").toLowerCase());
    logger.info("Creating table: {}", tableName);

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

      // Create a ShapefileTable instance
      ShapefileTable shapefileTable = new ShapefileTable(path.toFile());

      // Create a temporary table name for the shapefile data
      String shapefileTableName = "shapefile_data_" + System.currentTimeMillis();

      try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
        CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
        SchemaPlus rootSchema = calciteConnection.getRootSchema();

        // Register the shapefile table in the Calcite schema
        rootSchema.add(shapefileTableName, shapefileTable);

        // Create a table in PostgreSQL by selecting from the shapefile table
        String createTableSql = "CREATE TABLE " + tableName + " AS " +
            "SELECT * FROM " + shapefileTableName;

        logger.info("Executing SQL: {}", createTableSql);

        // Execute the DDL statement to create the table
        try (Statement statement = connection.createStatement()) {
          statement.execute(createTableSql);
        }

        // Set SRID on geometry column if specified
        try (Connection pgConnection = dataSource.getConnection();
            Statement stmt = pgConnection.createStatement()) {
          stmt.execute(String.format(
              "SELECT UpdateGeometrySRID('%s', 'geometry', %d)",
              tableName, databaseSrid));
        }

        // Verify that the table was created in PostgreSQL
        try (Connection pgConnection = dataSource.getConnection();
            Statement statement = pgConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = '" +
                    tableName + "')")) {
          if (!resultSet.next() || !resultSet.getBoolean(1)) {
            throw new WorkflowException("Failed to create table: " + tableName);
          }
        }

        // Verify that the table has data
        try (Connection pgConnection = dataSource.getConnection();
            Statement statement = pgConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT COUNT(*) FROM " + tableName)) {
          if (resultSet.next()) {
            int count = resultSet.getInt(1);
            logger.info("Imported {} rows to table: {}", count, tableName);
            if (count == 0) {
              logger.warn("No rows were imported from shapefile to table: {}", tableName);
            }
          }
        }
      }
    } finally {
      // Clean up thread local storage
      PostgresDdlExecutor.clearThreadLocalDataSource();
    }

    logger.info("Successfully imported shapefile to table: {}", tableName);
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
    return new StringJoiner(", ", ImportShapefile.class.getSimpleName() + "[", "]")
        .add("file=" + file)
        .add("fileSrid=" + fileSrid)
        .add("database=" + database)
        .add("databaseSrid=" + databaseSrid)
        .toString();
  }
}
