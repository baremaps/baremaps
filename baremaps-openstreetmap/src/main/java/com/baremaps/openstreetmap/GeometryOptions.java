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

package com.baremaps.openstreetmap;

import com.baremaps.openstreetmap.function.CoordinateMapBuilder;
import com.baremaps.openstreetmap.function.EntityGeometryBuilder;
import com.baremaps.openstreetmap.function.EntityProjectionTransformer;
import com.baremaps.openstreetmap.function.ReferenceMapBuilder;
import com.baremaps.openstreetmap.model.Entity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.locationtech.jts.geom.Coordinate;

/**
 * Configures the generation of geometries while reading OpenStreetMap entities.
 *
 * <p>
 * Building the geometry of a way or a relation requires the coordinates of nodes and the node
 * references of ways that appeared earlier in the file. The two maps hold that state; callers
 * provide them because a planet-sized import needs off-heap or on-disk implementations.
 *
 * @param coordinateMap the map storing the coordinates of nodes, indexed by node id
 * @param referenceMap the map storing the node ids of ways, indexed by way id
 * @param srid the spatial reference of the produced geometries; the file is always in WGS 84
 */
public record GeometryOptions(
    Map<Long, Coordinate> coordinateMap,
    Map<Long, List<Long>> referenceMap,
    int srid) {

  public static final int WGS84 = 4326;

  public GeometryOptions {
    Objects.requireNonNull(coordinateMap, "coordinateMap");
    Objects.requireNonNull(referenceMap, "referenceMap");
  }

  public GeometryOptions(Map<Long, Coordinate> coordinateMap, Map<Long, List<Long>> referenceMap) {
    this(coordinateMap, referenceMap, WGS84);
  }

  /**
   * Options backed by in-memory maps, suited to files small enough to fit in memory.
   *
   * @return the options
   */
  public static GeometryOptions inMemory() {
    return new GeometryOptions(new HashMap<>(), new HashMap<>());
  }

  /**
   * Creates the consumer that sets the geometry of entities.
   *
   * <p>
   * Entities must be fed in file order (nodes, then ways, then relations), because the maps are
   * populated and read by the same pass: a way can only be built once all its nodes have been seen.
   *
   * @return a consumer that sets the geometry of the entities it accepts
   */
  public Consumer<Entity> entityHandler() {
    return new CoordinateMapBuilder(coordinateMap)
        .andThen(new ReferenceMapBuilder(referenceMap))
        .andThen(new EntityGeometryBuilder(coordinateMap, referenceMap))
        .andThen(new EntityProjectionTransformer(WGS84, srid));
  }
}
