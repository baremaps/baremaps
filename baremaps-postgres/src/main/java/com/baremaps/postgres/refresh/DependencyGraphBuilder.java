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

import com.baremaps.postgres.refresh.DatabaseMetadataRetriever.DatabaseDependency;
import com.baremaps.postgres.refresh.DatabaseMetadataRetriever.DatabaseObject;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to build a directed dependency graph among DatabaseObject items.
 */
public class DependencyGraphBuilder {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DependencyGraphBuilder.class.getName());

  private DependencyGraphBuilder() {
    // Prevent instantiation
  }

  /**
   * Builds a directed graph using Guava for the given list of objects, then populates edges from
   * system catalogs.
   *
   * @param objects a list of database objects
   * @param dependencies a list of database dependencies
   * @return a directed graph of database objects and dependencies
   */
  public static MutableGraph<DatabaseObject> buildGraph(
      List<DatabaseObject> objects,
      List<DatabaseDependency> dependencies) throws SQLException {

    // Build a directed graph
    MutableGraph<DatabaseObject> graph = GraphBuilder
        .directed()
        .allowsSelfLoops(false)
        .build();

    // Add nodes for objects
    objects.forEach(graph::addNode);

    // Add edges for dependencies
    dependencies.forEach(dep -> graph.putEdge(dep.source(), dep.dependent()));

    return graph;
  }

  /**
   * Performs a topological sort of the given graph using Kahn's algorithm, so that dependencies
   * appear before their dependents.
   *
   * @param graph a directed graph of database objects
   * @return a list of database objects in topological order
   */
  public static List<DatabaseObject> topologicalSort(MutableGraph<DatabaseObject> graph) {
    var inDegree = new HashMap<DatabaseObject, Integer>();
    for (var node : graph.nodes()) {
      inDegree.put(node, 0);
    }

    for (var node : graph.nodes()) {
      for (var successor : graph.successors(node)) {
        inDegree.compute(successor, (k, v) -> v == null ? 1 : v + 1);
      }
    }

    var queue = new LinkedList<DatabaseObject>();
    for (var entry : inDegree.entrySet()) {
      if (entry.getValue() == 0) {
        queue.add(entry.getKey());
      }
    }

    var result = new ArrayList<DatabaseObject>();
    while (!queue.isEmpty()) {
      var current = queue.poll();
      result.add(current);
      for (var succ : graph.successors(current)) {
        var newVal = inDegree.get(succ) - 1;
        inDegree.put(succ, newVal);
        if (newVal == 0) {
          queue.add(succ);
        }
      }
    }

    return result;
  }

}
