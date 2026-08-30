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

package com.baremaps.workflow;

import static com.baremaps.utils.ObjectMapperUtils.objectMapper;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.baremaps.config.ConfigReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Checks that the workflows shipped in {@code examples} still parse.
 *
 * <p>
 * A task type is only reachable once it is listed in {@link Task}, and a workflow naming a type
 * that is not listed fails to parse rather than to run. This test is what makes that mismatch
 * visible: it caught {@code CreateGeonamesIndex} and {@code CreateIplocIndex} being dropped from
 * the list while two examples still used them.
 */
class ExampleWorkflowTest {

  private static final Path EXAMPLES = Path.of("..", "examples");

  static Stream<Path> workflows() throws IOException {
    try (var paths = Files.walk(EXAMPLES)) {
      return paths
          .filter(path -> path.getFileName().toString().matches("workflow\\.(js|json)"))
          .sorted()
          .toList()
          .stream();
    }
  }

  @ParameterizedTest
  @MethodSource("workflows")
  void parse(Path path) throws IOException {
    var workflow = objectMapper().readValue(new ConfigReader().read(path), Workflow.class);
    assertFalse(workflow.getSteps().isEmpty(), path + " declares no step");
    for (Step step : workflow.getSteps()) {
      assertNotNull(step.getId(), path + " declares a step without an id");
      List<Task> tasks = step.getTasks();
      assertFalse(tasks.isEmpty(), path + " declares step " + step.getId() + " without a task");
    }
  }
}
