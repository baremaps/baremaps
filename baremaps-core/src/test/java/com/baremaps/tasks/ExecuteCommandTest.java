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

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.workflow.WorkflowContext;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ExecuteCommandTest {

  @Test
  @Disabled("requires a shell")
  void execute() throws Exception {
    var path = Paths.get("file.txt").toAbsolutePath();
    new ExecuteCommand(String.format("echo test > %s", path)).execute(new WorkflowContext());
    assertTrue(Files.exists(path));
    assertTrue(Files.readString(path).contains("test"));
    Files.deleteIfExists(path);
  }
}
