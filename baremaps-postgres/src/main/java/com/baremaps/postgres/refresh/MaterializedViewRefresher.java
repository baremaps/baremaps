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

package com.baremaps.postgres.refresh;

import com.baremaps.postgres.refresh.DatabaseMetadataRetriever.DatabaseIndex;
import com.baremaps.postgres.refresh.DatabaseMetadataRetriever.DatabaseObject;
import com.baremaps.postgres.refresh.DatabaseMetadataRetriever.ObjectType;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to refresh materialized views in topological order: 1) drop indexes, 2) refresh MV,
 * 3) recreate indexes.
 */
public class MaterializedViewRefresher {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MaterializedViewRefresher.class.getName());

  private MaterializedViewRefresher() {
    // Prevent instantiation
  }

  /**
   * Traverse the sorted objects. When we hit a materialized view, drop its indexes, refresh it, and
   * recreate indexes.
   */
  public static void refreshMaterializedViews(Connection connection,
      List<DatabaseObject> sortedObjects) {
    for (var obj : sortedObjects) {
      if (obj.objectType() == ObjectType.MATERIALIZED_VIEW) {
        LOGGER.info("Refreshing materialized view: " + obj.schemaName() + "." + obj.objectName());
        try {
          var indexes =
              DatabaseMetadataRetriever.getIndexes(connection, obj.schemaName(), obj.objectName());
          dropIndexes(connection, indexes);
          refreshMaterializedView(connection, obj);
          recreateIndexes(connection, indexes);
        } catch (SQLException ex) {
          LOGGER.error("Error refreshing materialized view: " +
              obj.schemaName() + "." + obj.objectName(), ex);
        }
      }
    }
  }

  private static void dropIndexes(Connection connection, List<DatabaseIndex> indexes)
      throws SQLException {
    for (var idx : indexes) {
      LOGGER.info("Dropping index: " + idx.indexName());
      try (var st = connection.createStatement()) {
        var dropSql = String.format(
            "DROP INDEX IF EXISTS %s",
            idx.indexName());
        st.execute(dropSql);
      }
    }
  }

  private static void refreshMaterializedView(Connection connection, DatabaseObject mv)
      throws SQLException {
    var refreshSql = String.format(
        "REFRESH MATERIALIZED VIEW %s WITH DATA",
        mv.objectName());
    try (var st = connection.createStatement()) {
      st.execute(refreshSql);
    }
  }

  private static void recreateIndexes(Connection connection, List<DatabaseIndex> indexes)
      throws SQLException {
    for (var idx : indexes) {
      LOGGER.info("Recreating index: " + idx.indexName());
      try (var st = connection.createStatement()) {
        st.execute(idx.indexDef());
      }
    }
  }
}
