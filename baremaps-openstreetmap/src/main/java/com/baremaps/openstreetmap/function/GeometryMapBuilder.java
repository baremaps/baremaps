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



import com.baremaps.openstreetmap.model.Element;
import com.baremaps.openstreetmap.model.Entity;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.locationtech.jts.geom.Geometry;

/**
 * A consumer that stores the geometry of an element in a map.
 */
public class GeometryMapBuilder implements Consumer<Entity> {

  private final Map<Long, Geometry> geometryMap;

  private final Predicate<Entity> filter;

  /**
   * Constructs a {@code GeometryMapBuilder} with the provided map.
   *
   * @param geometryMap the geometry map
   * @param filter the entity filter
   */
  public GeometryMapBuilder(Map<Long, Geometry> geometryMap, Predicate<Entity> filter) {
    this.geometryMap = geometryMap;
    this.filter = filter;
  }

  /** {@inheritDoc} */
  @Override
  public void accept(Entity entity) {
    if (filter.test(entity) && entity instanceof Element element) {
      geometryMap.put(element.getId(), element.getGeometry());
    }
  }
}
