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

import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import com.baremaps.workflow.WorkflowException;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute a bash command.
 */
public class ExecuteCommand implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ExecuteCommand.class);

  private String command;

  /**
   * Constructs a {@code ExecuteCommand}.
   */
  public ExecuteCommand() {

  }

  /**
   * Constructs an {@code ExecuteCommand}.
   *
   * @param command the bash command
   */
  public ExecuteCommand(String command) {
    this.command = command;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(WorkflowContext context) throws Exception {
    logger.info("Executing command: {}", command);
    // The output of the command belongs in the log of the workflow that asked for it, and a
    // non-zero status has to fail the step rather than let the next one run on missing data.
    var exitCode = new ProcessBuilder("/bin/sh", "-c", command)
        .inheritIO()
        .start()
        .waitFor();
    if (exitCode != 0) {
      throw new WorkflowException(
          String.format("The command '%s' exited with code %d", command, exitCode));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new StringJoiner(", ", ExecuteCommand.class.getSimpleName() + "[", "]")
        .add("command='" + command + "'")
        .toString();
  }
}
