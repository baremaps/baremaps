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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to retrieve metadata about tables, views, and materialized views.
 */
public class DatabaseMetadataRetriever {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DatabaseMetadataRetriever.class.getName());

  private DatabaseMetadataRetriever() {
    // Prevent instantiation
  }

  /**
   * Retrieves all tables, views, and materialized views in the given schema.
   *
   * @param connection the database connection
   * @param schema the schema name
   * @return a list of DatabaseObject records
   * @throws SQLException if a database error occurs
   */
  public static List<DatabaseObject> getObjects(Connection connection, String schema)
      throws SQLException {
    var result = new ArrayList<DatabaseObject>();

    var sql = """
            SELECT c.oid,
                   c.relname,
                   c.relkind,
                   n.nspname
              FROM pg_class c
              JOIN pg_namespace n ON n.oid = c.relnamespace
             WHERE n.nspname = ?
               AND c.relkind IN ('r', 'v', 'm')
        """;

    try (var ps = connection.prepareStatement(sql)) {
      ps.setString(1, schema);
      try (var rs = ps.executeQuery()) {
        while (rs.next()) {
          var oid = rs.getLong("oid");
          var relName = rs.getString("relname");
          var relKind = rs.getString("relkind");
          var nspName = rs.getString("nspname");

          var objectType = switch (relKind) {
            case "r" -> ObjectType.TABLE;
            case "v" -> ObjectType.VIEW;
            case "m" -> ObjectType.MATERIALIZED_VIEW;
            default -> null;
          };

          var dbObj = new DatabaseObject(nspName, relName, objectType);
          result.add(dbObj);
        }
      }
    }

    LOGGER.info("Found " + result.size() + " objects in schema " + schema);
    return result;
  }

  /**
   * Retrieves dependencies between database objects in the given schema.
   *
   * @param connection the database connection
   * @param schema the schema name
   * @param objects a list of database objects
   * @return a list of DatabaseDependency records
   * @throws SQLException if a database error occurs
   */
  public static List<DatabaseDependency> getDependencies(Connection connection, String schema,
      List<DatabaseObject> objects) throws SQLException {
    var sql = """
            SELECT dependent_ns.nspname   AS dependent_schema,
                   dependent_c.relname    AS dependent_name,
                   source_ns.nspname      AS source_schema,
                   source_c.relname       AS source_name
              FROM pg_depend d
              JOIN pg_rewrite r
                ON r.oid = d.objid
              JOIN pg_class dependent_c
                ON r.ev_class = dependent_c.oid
              JOIN pg_namespace dependent_ns
                ON dependent_c.relnamespace = dependent_ns.oid
              JOIN pg_class source_c
                ON d.refobjid = source_c.oid
              JOIN pg_namespace source_ns
                ON source_c.relnamespace = source_ns.oid
             WHERE dependent_ns.nspname = ?
               AND source_ns.nspname = ?
        """;

    // Create a fast lookup by (schema + name).
    var lookupMap = new HashMap<String, DatabaseObject>();
    for (var obj : objects) {
      var key = obj.schemaName() + "." + obj.objectName();
      lookupMap.put(key, obj);
    }

    var result = new ArrayList<DatabaseDependency>();
    try (var ps = connection.prepareStatement(sql)) {
      ps.setString(1, schema);
      ps.setString(2, schema);
      try (var rs = ps.executeQuery()) {
        while (rs.next()) {
          var dependentSchema = rs.getString("dependent_schema");
          var dependentName = rs.getString("dependent_name");
          var sourceSchema = rs.getString("source_schema");
          var sourceName = rs.getString("source_name");

          var dependentKey = dependentSchema + "." + dependentName;
          var sourceKey = sourceSchema + "." + sourceName;

          var dependentObj = lookupMap.get(dependentKey);
          var sourceObj = lookupMap.get(sourceKey);

          if (dependentObj != null && sourceObj != null) {
            // Skip self-loop dependencies.
            if (!dependentObj.equals(sourceObj)) {
              result.add(new DatabaseDependency(sourceObj, dependentObj));
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Retrieves indexes for the given table or materialized view.
   *
   * @param connection the database connection
   * @param schema the schema name
   * @param tableName the table or materialized view name
   * @return a list of DatabaseIndex records
   * @throws SQLException if a database error occurs
   */
  public static List<DatabaseIndex> getIndexes(Connection connection, String schema,
      String tableName) throws SQLException {
    var sql = """
            SELECT indexname, indexdef
              FROM pg_indexes
             WHERE schemaname = ?
               AND tablename  = ?
        """;

    var result = new ArrayList<DatabaseIndex>();
    try (var ps = connection.prepareStatement(sql)) {
      ps.setString(1, schema);
      ps.setString(2, tableName);
      try (var rs = ps.executeQuery()) {
        while (rs.next()) {
          var indexName = rs.getString("indexname");
          var indexDef = rs.getString("indexdef");
          result.add(new DatabaseIndex(indexName, indexDef));
        }
      }
    }
    return result;
  }

  /**
   * Represents the type of database object.
   */
  public enum ObjectType {
    TABLE,
    VIEW,
    MATERIALIZED_VIEW
  }

  /**
   * A record representing a database object (table, view, materialized view).
   */
  public record DatabaseObject(
      String schemaName,
      String objectName,
      ObjectType objectType) {
  }

  /**
   * Record representing a dependency between two database objects.
   */
  public record DatabaseDependency(DatabaseObject source, DatabaseObject dependent) {
  }


  /**
   * Record representing a database index.
   */
  public record DatabaseIndex(String indexName, String indexDef) {
  }

}
