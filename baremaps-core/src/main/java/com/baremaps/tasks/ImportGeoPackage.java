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
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import com.baremaps.workflow.WorkflowException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Imports every feature table of a GeoPackage into a PostgreSQL table of the same name. */
public class ImportGeoPackage implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ImportGeoPackage.class);

  private Path file;
  private Integer fileSrid;
  private Object database;
  private Integer databaseSrid;

  /** Constructs a {@code ImportGeoPackage}. */
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

  @Override
  public void execute(WorkflowContext context) throws Exception {
    if (file == null) {
      throw new WorkflowException("GeoPackage path cannot be null");
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
    Path path = file.toAbsolutePath();
    logger.info("Importing GeoPackage from: {}", path);

    GeoPackageSchema schema = new GeoPackageSchema(path.toFile());
    Map<String, String> tables = new LinkedHashMap<>();
    for (String table : schema.getTableNames()) {
      tables.put(table, table);
    }
    if (tables.isEmpty()) {
      logger.warn("No tables found in GeoPackage: {}", path);
      return;
    }
    Map<String, Long> counts =
        PostgresImport.copy(context.getDataSource(database), schema, tables, databaseSrid);
    counts.forEach((table, count) -> logger.info("Imported {} rows to table: {}", count, table));
  }

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
