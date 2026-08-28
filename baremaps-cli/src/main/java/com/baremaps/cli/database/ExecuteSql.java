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
import com.baremaps.workflow.WorkflowContext;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(
    name = "execute-sql",
    description = "Execute SQL queries in the database.")
@SuppressWarnings("squid:S106")
public class ExecuteSql implements Callable<Integer> {

  @Mixin
  private Options options;

  @Option(names = {"--database"}, paramLabel = "DATABASE",
      description = "The JDBC url of the database.", required = true)
  private String database;

  @Option(names = {"--file"}, paramLabel = "FILE",
      description = "The SQL file to execute in the database.")
  private Path file;

  @Option(names = {"--parallel"}, paramLabel = "PARALLEL",
      description = "Executes the SQL queries in parallel.")
  private boolean parallel;

  @Override
  public Integer call() throws Exception {
    new com.baremaps.tasks.ExecuteSql(database, file.toAbsolutePath(),
        parallel).execute(new WorkflowContext());
    return 0;
  }
}
