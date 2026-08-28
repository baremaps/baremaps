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
import java.util.function.Consumer;

/** Changes the projection of the geometry of an entity via side-effects. */
public class EntityProjectionTransformer implements Consumer<Entity> {

  private final int sourceSrid;

  private final int targetSrid;

  private final ProjectionTransformer projectionTransformer;

  /**
   * Creates a consumer that reproject geometries with the provided SRIDs.
   *
   * @param sourceSrid the source SRID
   * @param targetSrid the target SRID
   */
  public EntityProjectionTransformer(int sourceSrid, int targetSrid) {
    this.sourceSrid = sourceSrid;
    this.targetSrid = targetSrid;
    this.projectionTransformer = new ProjectionTransformer(sourceSrid, targetSrid);
  }

  /** {@inheritDoc} */
  @Override
  public void accept(Entity entity) {
    if (sourceSrid != targetSrid
        && entity instanceof Element element
        && element.getGeometry() != null) {
      var geometry = projectionTransformer.transform(element.getGeometry());
      element.setGeometry(geometry);
    }
  }
}
