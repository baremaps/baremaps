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

package com.baremaps.openstreetmap.model;

import java.util.ArrayList;
import java.util.List;

/** A block of an OpenStreetMap PBF file holding nodes, ways and relations. */
public final class DataBlock extends Block {

  private final List<Node> nodes;
  private final List<Way> ways;
  private final List<Relation> relations;

  public DataBlock(List<Node> nodes, List<Way> ways, List<Relation> relations) {
    this.nodes = nodes;
    this.ways = ways;
    this.relations = relations;
  }

  public List<Node> getNodes() {
    return nodes;
  }

  public List<Way> getWays() {
    return ways;
  }

  public List<Relation> getRelations() {
    return relations;
  }

  @Override
  public List<Entity> entities() {
    var entities = new ArrayList<Entity>(nodes.size() + ways.size() + relations.size());
    entities.addAll(nodes);
    entities.addAll(ways);
    entities.addAll(relations);
    return entities;
  }
}
