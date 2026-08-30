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

package com.baremaps.cli.workflow;

import static com.baremaps.utils.ObjectMapperUtils.objectMapper;

import com.baremaps.tasks.LogMessage;
import com.baremaps.workflow.Step;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "init",
    description = "Initialize a workflow.")
public class Init implements Callable<Integer> {

  private static final Logger logger = LoggerFactory.getLogger(Init.class);

  @Option(names = {"--file"}, paramLabel = "FILE", description = "A workflow file.",
      required = true)
  private Path file;

  @Override
  public Integer call() throws Exception {
    var workflow = new com.baremaps.workflow.Workflow(
        List.of(new Step("hello", List.of(), List.of(new LogMessage("Hello World!")))));
    Files.write(file, objectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(workflow));
    logger.info("Workflow initialized: {}", file);
    return 0;
  }
}
