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

import com.baremaps.openstreetmap.model.Bound;
import com.baremaps.openstreetmap.model.Entity;
import com.baremaps.openstreetmap.model.Header;
import com.baremaps.openstreetmap.model.Node;
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.openstreetmap.model.Way;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.locationtech.jts.geom.Coordinate;

/**
 * A consumer that sets the geometry of the entities it accepts, dispatching each one to the builder
 * of its kind.
 *
 * <p>
 * The maps must already hold the nodes and ways the entity refers to, which is why entities have to
 * be fed in file order. Recording them is the job of {@link EntityMapBuilder}, kept separate so
 * that a caller reading from an already populated database does not write back into it.
 */
public class EntityGeometryBuilder implements Consumer<Entity> {

  private final NodeGeometryBuilder nodeGeometryBuilder = new NodeGeometryBuilder();
  private final WayGeometryBuilder wayGeometryBuilder;
  private final RelationGeometryBuilder relationGeometryBuilder;

  /**
   * Constructs a consumer that uses the provided maps to build geometries.
   *
   * @param coordinateMap the coordinates of nodes, indexed by node id
   * @param referenceMap the node ids of ways, indexed by way id
   */
  public EntityGeometryBuilder(
      Map<Long, Coordinate> coordinateMap,
      Map<Long, List<Long>> referenceMap) {
    this.wayGeometryBuilder = new WayGeometryBuilder(coordinateMap);
    this.relationGeometryBuilder = new RelationGeometryBuilder(coordinateMap, referenceMap);
  }

  @Override
  public void accept(Entity entity) {
    switch (entity) {
      case Node node -> nodeGeometryBuilder.accept(node);
      case Way way -> wayGeometryBuilder.accept(way);
      case Relation relation -> relationGeometryBuilder.accept(relation);
      case Header ignored -> {
        // The header and the bounds describe the file, not a feature of the map.
      }
      case Bound ignored -> {
        // Idem.
      }
    }
  }
}
