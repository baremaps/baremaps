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

import com.baremaps.calcite.shapefile.ShapefileSchema;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import com.baremaps.workflow.WorkflowException;
import java.nio.file.Path;
import java.util.Map;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Imports a shapefile into a PostgreSQL table named after the file. */
public class ImportShapefile implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ImportShapefile.class);

  private Path file;
  private Integer fileSrid;
  private Object database;
  private Integer databaseSrid;

  /** Constructs a {@code ImportShapefile}. */
  public ImportShapefile() {}

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

  @Override
  public void execute(WorkflowContext context) throws Exception {
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
    Path path = file.toAbsolutePath();
    logger.info("Importing shapefile from: {}", path);

    ShapefileSchema schema = new ShapefileSchema(path.toFile());
    String sourceName = schema.getTableNames().iterator().next();
    Map<String, Long> counts = PostgresImport.copy(context.getDataSource(database), schema,
        Map.of(sourceName, sourceName), databaseSrid);
    counts.forEach((table, count) -> logger.info("Imported {} rows to table: {}", count, table));
  }

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
