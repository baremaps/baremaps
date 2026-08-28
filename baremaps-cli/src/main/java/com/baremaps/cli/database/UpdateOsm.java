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



import com.baremaps.cli.Options;
import com.baremaps.tasks.UpdateOsmDatabase;
import com.baremaps.workflow.WorkflowContext;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(
    name = "update-osm",
    description = "Update OpenStreetMap data in Postgres.")
@SuppressWarnings("squid:S106")
public class UpdateOsm implements Callable<Integer> {

  @Mixin
  private Options options;

  @Option(names = {"--database"}, paramLabel = "DATABASE",
      description = "The JDBC url of Postgres.", required = true)
  private String database;

  @Option(names = {"--srid"}, paramLabel = "SRID",
      description = "The projection used by the database.")
  private int srid = 3857;

  @Option(names = {"--replication-url"}, paramLabel = "REPLICATION_URL",
      description = "The replication url of the OpenStreetMap server.")
  private String replicationUrl = "https://planet.osm.org/replication/hour";

  @Override
  public Integer call() throws Exception {
    new UpdateOsmDatabase(database, srid, replicationUrl)
        .execute(new WorkflowContext());
    return 0;
  }
}
