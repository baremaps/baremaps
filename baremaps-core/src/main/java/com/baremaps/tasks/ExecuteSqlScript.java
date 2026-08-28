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
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute a SQL script (multiple statements).
 */
public class ExecuteSqlScript implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ExecuteSqlScript.class);

  private Object database;

  private Path file;

  /**
   * Constructs a {@code ExecuteSqlScript}.
   */
  public ExecuteSqlScript() {

  }

  /**
   * Constructs an {@code ExecuteSqlScript}.
   *
   * @param database the database
   * @param file the SQL file
   */
  public ExecuteSqlScript(Object database, Path file) {
    this.database = database;
    this.file = file;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(WorkflowContext context) throws Exception {
    var script = Files.readString(file);
    var dataSource = context.getDataSource(database);
    try (var connection = dataSource.getConnection();
        var statement = connection.createStatement()) {
      statement.execute(script);
    } catch (SQLException e) {
      throw new WorkflowException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new StringJoiner(", ", ExecuteSqlScript.class.getSimpleName() + "[", "]")
        .add("database=" + database)
        .add("file=" + file)
        .toString();
  }
}
