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
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute a SQL query (single statement).
 */
public class ExecuteSql implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ExecuteSql.class);

  private Object database;

  private Path file;

  private boolean parallel = false;

  /**
   * Constructs a {@code ExecuteSql}.
   */
  public ExecuteSql() {

  }

  /**
   * Constructs an {@code ExecuteSql}.
   *
   * @param database the database
   * @param file the SQL file
   * @param parallel whether to execute the queries in parallel
   */
  public ExecuteSql(Object database, Path file, boolean parallel) {
    this.database = database;
    this.file = file;
    this.parallel = parallel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(WorkflowContext context) throws Exception {
    var script = clean(Files.readString(file));
    var queries = split(script);
    if (parallel) {
      queries = queries.parallel();
    }
    queries.forEach(
        query -> {
          var dataSource = context.getDataSource(database);
          try (var connection = dataSource.getConnection()) {
            logger.info("Execute SQL query: {}", query.replaceAll("\\s+", " "));
            connection.createStatement().execute(query);
          } catch (SQLException e) {
            logger.error("Failed to execute query: {}", query.replaceAll("\\s+", " "));
            throw new WorkflowException(e);
          }
        });
  }

  /**
   * Split a SQL string into multiple SQL statements.
   *
   * @param sql The SQL string.
   * @return The SQL statements.
   */
  @SuppressWarnings({"squid:S5852", "squid:S5998"})
  public static Stream<String> split(String sql) {
    return Arrays.stream(sql.split("\\s*;\\s*(?=(?:[^']*'[^']*')*[^']*$)"));
  }

  /**
   * Remove comments from a SQL string.
   *
   * @param sql The SQL string.
   * @return The SQL string without comments.
   */
  public static String clean(String sql) {
    var result = sql;

    // remove single line comments
    var singleLineCommentPattern = Pattern.compile("--.*", Pattern.MULTILINE);
    var singleLineCommentMatcher = singleLineCommentPattern.matcher(result);
    result = singleLineCommentMatcher.replaceAll("");

    // remove multi line comments
    var multiLineCommentPattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    var multiLineMatcher = multiLineCommentPattern.matcher(result);
    result = multiLineMatcher.replaceAll("");

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new StringJoiner(", ", ExecuteSql.class.getSimpleName() + "[", "]")
        .add("database=" + database)
        .add("file=" + file)
        .add("parallel=" + parallel)
        .toString();
  }
}
