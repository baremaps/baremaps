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

package com.baremaps.cli.map;

import com.baremaps.config.ConfigReader;
import com.baremaps.maplibre.map.MapCompiler;
import com.baremaps.maplibre.map.MapSpec;
import com.baremaps.maplibre.map.SchemaCompiler;
import com.baremaps.tasks.ExecuteSqlScript;
import com.baremaps.workflow.Step;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.Workflow;
import com.baremaps.workflow.WorkflowExecutor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Creates the schema a map is read from.
 *
 * <p>
 * The order used to be written out by hand, in a workflow that listed the extensions, then the
 * tables, then a schema file per layer, then the levels each generalized layer is read from. All of
 * that except the first two is already in the map: which layers there are, which schema each is
 * read out of, and which of them are generalized. Only the prerequisites are not, so only those are
 * declared.
 */
@Command(name = "create", description = "Create the schema the map is read from.")
public class Create implements Callable<Integer> {

  private static final Logger logger = LoggerFactory.getLogger(Create.class);

  private final ConfigReader configReader = new ConfigReader();

  @Option(names = {"--map"}, paramLabel = "MAP", description = "The map specification file.",
      required = true)
  private Path mapPath;

  @Override
  public Integer call() throws Exception {
    var spec = configReader.read(mapPath, MapSpec.class);
    var directory = mapPath.toAbsolutePath().getParent();
    var database = spec.database();

    var tasks = new ArrayList<Task>();
    for (var schema : spec.schema() == null ? List.<String>of() : spec.schema()) {
      tasks.add(new ExecuteSqlScript(database, directory.resolve(schema)));
    }
    for (var schema : MapCompiler.schemas(spec)) {
      tasks.add(new ExecuteSqlScript(database, directory.resolve(schema)));
    }

    // The levels below the generalization zoom are written rather than stored, so they are handed
    // to the same task through a file that does not outlive the run.
    var generalization = SchemaCompiler.sql(spec);
    Path generated = null;
    if (!generalization.isBlank()) {
      generated = Files.createTempFile("baremaps-generalize-", ".sql");
      Files.writeString(generated, generalization);
      tasks.add(new ExecuteSqlScript(database, generated));
    }

    logger.info("Creating the schema of {} in {} steps", mapPath, tasks.size());
    try {
      new WorkflowExecutor(new Workflow(List.of(new Step("create", List.of(), tasks)))).execute();
    } finally {
      if (generated != null) {
        Files.deleteIfExists(generated);
      }
    }
    logger.info("Schema created.");
    return 0;
  }
}
