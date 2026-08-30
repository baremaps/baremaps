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

import com.baremaps.postgres.refresh.MaterializedViewRefresher;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Refresh the materialized views of a database.
 */
public class RefreshMaterializedViews implements Task {

  private static final Logger logger = LoggerFactory.getLogger(RefreshMaterializedViews.class);

  private Object database;

  /**
   * Constructs a {@code RefreshMaterializedViews}.
   */
  public RefreshMaterializedViews() {
    // Default constructor
  }

  /**
   * Constructs a {@code RefreshMaterializedViews}.
   *
   * @param database the database
   */
  public RefreshMaterializedViews(Object database) {
    this.database = database;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(WorkflowContext context) throws Exception {
    new MaterializedViewRefresher(context.getDataSource(database)).refresh();
    logger.info("Done refreshing materialized views.");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new StringJoiner(", ", RefreshMaterializedViews.class.getSimpleName() + "[", "]")
        .add("database=" + database)
        .toString();
  }
}
