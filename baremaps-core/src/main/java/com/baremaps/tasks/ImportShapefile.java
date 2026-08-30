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
import java.nio.file.Path;
import java.util.Map;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Imports a shapefile into a PostgreSQL table named after the file. */
public class ImportShapefile implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ImportShapefile.class);

  private Path file;
  // Kept as part of the workflow schema, but not applied: the import copies geometries verbatim
  // and labels the resulting columns with databaseSrid rather than reprojecting them.
  private Integer fileSrid;
  private Object database;
  private Integer databaseSrid;

  /** Constructs a {@code ImportShapefile}. */
  public ImportShapefile() {}

  /**
   * Constructs an {@code ImportShapefile}.
   *
   * @param file the shapefile file
   * @param fileSrid the SRID of the shapefile, recorded but not yet applied: the import copies the
   *        geometries as they are and labels them with {@code databaseSrid}
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
    Path path = Task.required(file, "file").toAbsolutePath();
    logger.info("Importing shapefile from: {}", path);

    var schema = new ShapefileSchema(path.toFile());
    String sourceName = schema.getTableNames().iterator().next();
    PostgresImport.copyAndReport(
        context.getDataSource(Task.required(database, "database")), schema,
        Map.of(sourceName, sourceName), Task.required(databaseSrid, "databaseSrid"));
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
