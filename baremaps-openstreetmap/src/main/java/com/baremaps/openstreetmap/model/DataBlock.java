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



import java.util.List;

/** Represents a data block in an OpenStreetMap dataset. */
public final class DataBlock extends Block {

  private final List<Node> denseNodes;
  private final List<Node> nodes;
  private final List<Way> ways;
  private final List<Relation> relations;

  /**
   * Constructs an OpenStreetMap {@code DataBlock} with the specified parameters.
   *
   * @param blob the blob
   * @param denseNodes the dense nodes
   * @param nodes the nodes
   * @param ways the ways
   * @param relations the relations
   */
  public DataBlock(Blob blob, List<Node> denseNodes, List<Node> nodes, List<Way> ways,
      List<Relation> relations) {
    super(blob);
    this.denseNodes = denseNodes;
    this.nodes = nodes;
    this.ways = ways;
    this.relations = relations;
  }

  /**
   * Returns the dense nodes.
   *
   * @return the dense nodes
   */
  public List<Node> getDenseNodes() {
    return denseNodes;
  }

  /**
   * Returns the nodes.
   *
   * @return the nodes
   */
  public List<Node> getNodes() {
    return nodes;
  }

  /**
   * Returns the ways.
   *
   * @return the ways
   */
  public List<Way> getWays() {
    return ways;
  }

  /**
   * Returns the relations.
   *
   * @return the relations
   */
  public List<Relation> getRelations() {
    return relations;
  }

}
