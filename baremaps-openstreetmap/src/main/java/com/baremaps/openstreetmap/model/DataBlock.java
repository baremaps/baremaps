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

import java.util.AbstractList;
import java.util.List;

/**
 * A block of an OpenStreetMap PBF file holding nodes, ways and relations.
 *
 * @param nodes the nodes of the block
 * @param ways the ways of the block
 * @param relations the relations of the block
 */
public record DataBlock(List<Node> nodes, List<Way> ways, List<Relation> relations)
    implements
      Block {

  /**
   * Returns a view over the three lists rather than a copy of them: a planet import walks the
   * entities of every block it decodes, and copying them would double the allocation of the whole
   * file.
   */
  @Override
  public List<Entity> entities() {
    return new AbstractList<>() {

      @Override
      public Entity get(int index) {
        if (index < nodes.size()) {
          return nodes.get(index);
        }
        if (index < nodes.size() + ways.size()) {
          return ways.get(index - nodes.size());
        }
        return relations.get(index - nodes.size() - ways.size());
      }

      @Override
      public int size() {
        return nodes.size() + ways.size() + relations.size();
      }
    };
  }
}
