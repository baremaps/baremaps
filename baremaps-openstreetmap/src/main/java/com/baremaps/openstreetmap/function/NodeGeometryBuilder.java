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
import java.util.function.Consumer;
import org.locationtech.jts.geom.*;

/**
 * A consumer that builds and sets a node geometry via side effects.
 */
public class NodeGeometryBuilder implements Consumer<Entity> {

  private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

  /** {@inheritDoc} */
  @Override
  public void accept(Entity entity) {
    if (entity instanceof Node node) {
      Point point = geometryFactory.createPoint(new Coordinate(node.getLon(), node.getLat()));
      node.setGeometry(point);
    }
  }
}
