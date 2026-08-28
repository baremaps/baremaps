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



import com.baremaps.openstreetmap.model.*;
import com.baremaps.openstreetmap.stream.StreamException;
import java.util.Optional;
import java.util.function.Function;
import org.locationtech.jts.geom.Geometry;

/** A function that maps an {@code Entity} to its {@code Geometry}. */
public class EntityToGeometryMapper implements Function<Entity, Optional<Geometry>> {

  @Override
  public Optional<Geometry> apply(Entity entity) {
    try {
      if (entity instanceof Node node) {
        return Optional.ofNullable(node.getGeometry());
      } else if (entity instanceof Way way) {
        return Optional.ofNullable(way.getGeometry());
      } else if (entity instanceof Relation relation) {
        return Optional.ofNullable(relation.getGeometry());
      } else if (entity instanceof Header) {
        return Optional.empty();
      } else if (entity instanceof Bound) {
        return Optional.empty();
      } else {
        throw new StreamException("Unknown entity type.");
      }
    } catch (Exception e) {
      throw new StreamException(e);
    }
  }
}
