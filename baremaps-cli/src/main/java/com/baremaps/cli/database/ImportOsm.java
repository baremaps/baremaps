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

package com.baremaps.cli.database;

import com.baremaps.tasks.ImportOsmPbf;
import com.baremaps.workflow.WorkflowContext;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "import-osm",
    description = "Import OpenStreetMap data in Postgres.")
public class ImportOsm implements Callable<Integer> {

  // An import starts from a clean slate: the tables it needs are recreated and emptied. An update
  // is the command to run to add data to an existing database.
  private static final boolean REPLACE_EXISTING = true;
  private static final boolean TRUNCATE_TABLES = true;

  @Option(names = {"--file"}, paramLabel = "FILE",
      description = "The PBF file to import in the database.", required = true)
  private Path file;

  @Option(names = {"--database"}, paramLabel = "DATABASE",
      description = "The JDBC url of Postgres.", required = true)
  private String database;

  @Option(names = {"--srid"}, paramLabel = "SRID",
      description = "The projection used by the database.")
  private int srid = 3857;

  @Override
  public Integer call() throws Exception {
    new ImportOsmPbf(file.toAbsolutePath(), database, srid, REPLACE_EXISTING, TRUNCATE_TABLES)
        .execute(new WorkflowContext());
    return 0;
  }
}
