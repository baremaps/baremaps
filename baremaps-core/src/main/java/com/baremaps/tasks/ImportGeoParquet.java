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

import com.baremaps.calcite.geoparquet.GeoParquetSchema;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import com.baremaps.workflow.WorkflowException;
import java.net.URI;
import java.util.Map;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Imports a GeoParquet file into a PostgreSQL table. */
public class ImportGeoParquet implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ImportGeoParquet.class);

  private URI uri;
  private String tableName;
  private Object database;
  private Integer databaseSrid;

  /** Constructs a {@code ImportGeoParquet}. */
  public ImportGeoParquet() {}

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

  @Override
  public void execute(WorkflowContext context) throws Exception {
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

    Map<String, Long> counts = PostgresImport.copy(context.getDataSource(database),
        new GeoParquetSchema(uri), Map.of(GeoParquetSchema.TABLE_NAME, tableName), databaseSrid);
    counts.forEach((table, count) -> logger.info("Imported {} rows to table: {}", count, table));
  }

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
