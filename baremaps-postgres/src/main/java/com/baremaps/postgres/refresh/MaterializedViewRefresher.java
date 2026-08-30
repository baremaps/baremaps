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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Refreshes every materialized view of a schema in dependency order.
 *
 * <p>
 * A materialized view built on another one has to be refreshed after it, or it repopulates itself
 * from stale rows. Postgres does not order the work, so the views are read from the system
 * catalogs, sorted topologically, and refreshed one at a time.
 *
 * <p>
 * Refreshing a view rewrites its heap, and doing so with the indexes in place makes Postgres
 * maintain every one of them row by row. Dropping the indexes first and recreating them afterwards
 * is faster for the full rewrite this performs.
 */
public final class MaterializedViewRefresher {

  private static final Logger logger = LoggerFactory.getLogger(MaterializedViewRefresher.class);

  private final DataSource dataSource;

  /**
   * Constructs a {@code MaterializedViewRefresher}.
   *
   * @param dataSource the data source
   */
  public MaterializedViewRefresher(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /** Refreshes the materialized views of the schema the connection defaults to. */
  public void refresh() throws SQLException {
    try (var connection = dataSource.getConnection()) {
      refresh(connection, connection.getSchema());
    }
  }

  /**
   * Refreshes the materialized views of the given schema.
   *
   * @param schema the schema holding the views
   */
  public void refresh(String schema) throws SQLException {
    try (var connection = dataSource.getConnection()) {
      refresh(connection, schema);
    }
  }

  /**
   * Refreshes the views of a schema, stopping at the first failure.
   *
   * <p>
   * Carrying on past a failure would refresh views that read from the one that failed, quietly
   * republishing its stale rows as if they were current.
   */
  private void refresh(Connection connection, String schema) throws SQLException {
    var objects = catalogObjects(connection, schema);
    logger.info("Found {} objects in schema {}", objects.size(), schema);
    var order = refreshOrder(objects, dependencies(connection, schema, objects));
    for (var object : order) {
      if (object.kind() == Kind.MATERIALIZED_VIEW) {
        refresh(connection, object);
      }
    }
  }

  private void refresh(Connection connection, CatalogObject view) throws SQLException {
    logger.info("Refreshing materialized view {}", view.qualifiedName());
    var indexes = indexes(connection, view);
    dropIndexes(connection, view.schema(), indexes);
    try {
      execute(connection,
          "REFRESH MATERIALIZED VIEW %s WITH DATA".formatted(view.qualifiedName()));
    } finally {
      // The indexes are already gone; recreating them even when the refresh fails is what keeps a
      // failed run from silently leaving the view unindexed.
      createIndexes(connection, indexes);
    }
  }

  private void dropIndexes(Connection connection, String schema, List<Index> indexes)
      throws SQLException {
    for (var index : indexes) {
      logger.info("Dropping index {}", index.name());
      execute(connection, "DROP INDEX IF EXISTS %s.%s"
          .formatted(quote(schema), quote(index.name())));
    }
  }

  private void createIndexes(Connection connection, List<Index> indexes) throws SQLException {
    for (var index : indexes) {
      logger.info("Recreating index {}", index.name());
      // pg_indexes hands back a complete, already quoted CREATE INDEX statement.
      execute(connection, index.definition());
    }
  }

  private static void execute(Connection connection, String sql) throws SQLException {
    try (var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  /** Reads the tables, views and materialized views of a schema. */
  private static List<CatalogObject> catalogObjects(Connection connection, String schema)
      throws SQLException {
    var sql = """
            SELECT n.nspname,
                   c.relname,
                   c.relkind
              FROM pg_class c
              JOIN pg_namespace n ON n.oid = c.relnamespace
             WHERE n.nspname = ?
               AND c.relkind IN ('r', 'v', 'm')
        """;
    var objects = new ArrayList<CatalogObject>();
    try (var statement = connection.prepareStatement(sql)) {
      statement.setString(1, schema);
      try (var result = statement.executeQuery()) {
        while (result.next()) {
          objects.add(new CatalogObject(
              result.getString(1),
              result.getString(2),
              Kind.forRelKind(result.getString(3))));
        }
      }
    }
    return objects;
  }

  /**
   * Reads the dependencies between the given objects, as {@code source -> dependent} pairs.
   *
   * <p>
   * A view records its sources through the rewrite rule that defines it, which is why the query
   * joins pg_depend to pg_rewrite. Dependencies that reach outside the schema, and the self
   * dependency every rewrite rule has on its own view, are dropped.
   */
  private static Map<CatalogObject, List<CatalogObject>> dependencies(
      Connection connection, String schema, List<CatalogObject> objects) throws SQLException {
    var sql = """
            SELECT source_ns.nspname      AS source_schema,
                   source_c.relname       AS source_name,
                   dependent_ns.nspname   AS dependent_schema,
                   dependent_c.relname    AS dependent_name
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
    var byName = new HashMap<String, CatalogObject>();
    for (var object : objects) {
      byName.put(object.qualifiedName(), object);
    }
    var dependents = new HashMap<CatalogObject, List<CatalogObject>>();
    try (var statement = connection.prepareStatement(sql)) {
      statement.setString(1, schema);
      statement.setString(2, schema);
      try (var result = statement.executeQuery()) {
        while (result.next()) {
          var source = byName.get(CatalogObject.qualifiedName(
              result.getString("source_schema"), result.getString("source_name")));
          var dependent = byName.get(CatalogObject.qualifiedName(
              result.getString("dependent_schema"), result.getString("dependent_name")));
          if (source != null && dependent != null && !source.equals(dependent)) {
            dependents.computeIfAbsent(source, key -> new ArrayList<>()).add(dependent);
          }
        }
      }
    }
    return dependents;
  }

  /**
   * Orders the objects so that each one comes after everything it reads from, using Kahn's
   * algorithm.
   *
   * @throws IllegalStateException if the dependencies contain a cycle, since no order then
   *         satisfies every view and refreshing a subset would leave the rest quietly stale
   */
  private static List<CatalogObject> refreshOrder(
      List<CatalogObject> objects, Map<CatalogObject, List<CatalogObject>> dependents) {
    // LinkedHashMap keeps the catalog order among objects that do not depend on each other, so a
    // run refreshes the same views in the same sequence every time.
    var incoming = new LinkedHashMap<CatalogObject, Integer>();
    objects.forEach(object -> incoming.put(object, 0));
    dependents.values().stream().flatMap(List::stream)
        .forEach(dependent -> incoming.merge(dependent, 1, Integer::sum));

    Queue<CatalogObject> ready = new ArrayDeque<>();
    incoming.forEach((object, count) -> {
      if (count == 0) {
        ready.add(object);
      }
    });

    var order = new ArrayList<CatalogObject>(objects.size());
    while (!ready.isEmpty()) {
      var current = ready.poll();
      order.add(current);
      for (var dependent : dependents.getOrDefault(current, List.of())) {
        if (incoming.merge(dependent, -1, Integer::sum) == 0) {
          ready.add(dependent);
        }
      }
    }

    if (order.size() != objects.size()) {
      throw new IllegalStateException(
          "The views of the schema depend on each other cyclically and cannot be ordered");
    }
    return order;
  }

  /** Reads the indexes of a table or materialized view. */
  private static List<Index> indexes(Connection connection, CatalogObject object)
      throws SQLException {
    var sql = """
            SELECT indexname, indexdef
              FROM pg_indexes
             WHERE schemaname = ?
               AND tablename  = ?
        """;
    var indexes = new ArrayList<Index>();
    try (var statement = connection.prepareStatement(sql)) {
      statement.setString(1, object.schema());
      statement.setString(2, object.name());
      try (var result = statement.executeQuery()) {
        while (result.next()) {
          indexes.add(new Index(result.getString(1), result.getString(2)));
        }
      }
    }
    return indexes;
  }

  /**
   * Quotes an identifier read from the catalogs. Schema, view and index names are not restricted to
   * lower case words, and an unquoted name that needs quoting refers to a different object or fails
   * to parse.
   */
  private static String quote(String identifier) {
    return '"' + identifier.replace("\"", "\"\"") + '"';
  }

  /** The kind of object a row of pg_class describes. */
  private enum Kind {
    TABLE,
    VIEW,
    MATERIALIZED_VIEW;

    static Kind forRelKind(String relKind) {
      return switch (relKind) {
        case "r" -> TABLE;
        case "v" -> VIEW;
        case "m" -> MATERIALIZED_VIEW;
        default -> throw new IllegalStateException("Unexpected relkind: " + relKind);
      };
    }
  }

  /** A table, view or materialized view of the schema being refreshed. */
  private record CatalogObject(String schema, String name, Kind kind) {

    String qualifiedName() {
      return qualifiedName(schema, name);
    }

    static String qualifiedName(String schema, String name) {
      return quote(schema) + "." + quote(name);
    }
  }

  /** An index of a materialized view, with the statement that recreates it. */
  private record Index(String name, String definition) {
  }
}
