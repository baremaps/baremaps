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

package com.baremaps.openstreetmap.function;

import com.baremaps.openstreetmap.model.Entity;
import com.baremaps.openstreetmap.model.Node;
import com.baremaps.openstreetmap.model.Way;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.locationtech.jts.geom.Coordinate;

/**
 * A consumer that records what later entities will need to build their geometry: the coordinate of
 * every node, and the node references of every way.
 */
public class EntityMapBuilder implements Consumer<Entity> {

  private final Map<Long, Coordinate> coordinateMap;
  private final Map<Long, List<Long>> referenceMap;

  /**
   * Constructs a consumer that fills the provided maps.
   *
   * @param coordinateMap the map of coordinates, indexed by node id
   * @param referenceMap the map of node ids, indexed by way id
   */
  public EntityMapBuilder(
      Map<Long, Coordinate> coordinateMap,
      Map<Long, List<Long>> referenceMap) {
    this.coordinateMap = coordinateMap;
    this.referenceMap = referenceMap;
  }

  @Override
  public void accept(Entity entity) {
    switch (entity) {
      case Node node -> coordinateMap.put(node.getId(),
          new Coordinate(node.getLon(), node.getLat()));
      case Way way -> referenceMap.put(way.getId(), way.getNodes());
      default -> {
        // Nothing a relation, a header or the bounds hold is needed to build a later geometry.
      }
    }
  }
}
