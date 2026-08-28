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
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.openstreetmap.model.Way;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.locationtech.jts.geom.*;

/** A consumer that builds and sets the geometry of OpenStreetMap entities via side effects. */
public class EntityGeometryBuilder implements Consumer<Entity> {

  private final Consumer<Entity> nodeGeometryBuilder;
  private final Consumer<Entity> wayGeometryBuilder;
  private final Consumer<Entity> relationMultiPolygonBuilder;

  /**
   * Constructs a consumer that uses the provided caches to create and set geometries.
   *
   * @param coordinateMap the coordinate cache
   * @param referenceMap the reference cache
   */
  public EntityGeometryBuilder(
      Map<Long, Coordinate> coordinateMap,
      Map<Long, List<Long>> referenceMap) {
    this.nodeGeometryBuilder = new NodeGeometryBuilder();
    this.wayGeometryBuilder = new WayGeometryBuilder(coordinateMap);
    this.relationMultiPolygonBuilder = new RelationMultiPolygonBuilder(coordinateMap, referenceMap);
  }

  /**
   * Returns true if the relation describes an area. Only multipolygons and boundaries do:
   * boundaries are administrative areas whose rings are shared with neighbours, and other relation
   * types (routes, restrictions, ...) have no single geometry. Coastline relations are excluded
   * because the coastline of a landmass is too large to assemble in memory.
   */
  private static boolean isMultiPolygon(Relation relation) {
    var tags = relation.getTags();
    if ("coastline".equals(tags.get("natural"))) {
      return false;
    }
    return "multipolygon".equals(tags.get("type")) || "boundary".equals(tags.get("type"));
  }

  /** {@inheritDoc} */
  @Override
  public void accept(Entity entity) {
    if (entity instanceof Node node) {
      nodeGeometryBuilder.accept(node);
    } else if (entity instanceof Way way) {
      wayGeometryBuilder.accept(way);
    } else if (entity instanceof Relation relation && isMultiPolygon(relation)) {
      relationMultiPolygonBuilder.accept(relation);
    }
  }

}
