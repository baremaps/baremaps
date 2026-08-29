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
import com.baremaps.openstreetmap.model.Way;
import com.baremaps.openstreetmap.utils.BatchMap;
import com.baremaps.openstreetmap.utils.GeometryUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A consumer that builds and sets a way geometry via side effects. */
public class WayGeometryBuilder implements Consumer<Entity> {

  private static final Logger logger = LoggerFactory.getLogger(WayGeometryBuilder.class);

  private final Map<Long, Coordinate> coordinateMap;

  /**
   * Constructs a way geometry builder.
   *
   * @param coordinateMap the coordinates map
   */
  public WayGeometryBuilder(Map<Long, Coordinate> coordinateMap) {
    this.coordinateMap = coordinateMap;
  }

  @Override
  public void accept(Entity entity) {
    if (entity instanceof Way way) {
      try {
        way.setGeometry(build(way));
      } catch (Exception e) {
        logger.debug("Unable to build the geometry for way #" + way.getId(), e);
        way.setGeometry(GeometryUtils.GEOMETRY_FACTORY_WGS84.createEmpty(0));
      }
    }
  }

  private org.locationtech.jts.geom.Geometry build(Way way) {
    LineString line = lineString(way.getNodes(), coordinateMap);
    if (line.isEmpty()) {
      return null;
    }
    // A closed way is an area unless its tags say it is a linear feature, in which case it is a
    // loop (e.g. a roundabout). See https://wiki.openstreetmap.org/wiki/Way
    if (!line.isClosed()
        || way.getTags().containsKey("railway")
        || way.getTags().containsKey("highway")
        || way.getTags().containsKey("barrier")) {
      return line;
    }
    Polygon polygon = GeometryUtils.GEOMETRY_FACTORY_WGS84.createPolygon(line.getCoordinates());
    return polygon.isValid() ? polygon : new GeometryFixer(polygon).getResult();
  }

  /**
   * Builds the line of a sequence of nodes. Nodes without a known coordinate are skipped, and
   * consecutive duplicate coordinates are collapsed since JTS rejects them in rings.
   *
   * @param nodes the node ids
   * @param coordinateMap the coordinates of nodes
   * @return the line, possibly empty
   */
  static LineString lineString(List<Long> nodes, Map<Long, Coordinate> coordinateMap) {
    List<Coordinate> list = new ArrayList<>(nodes.size());
    Coordinate previous = null;
    for (Coordinate coordinate : coordinates(nodes, coordinateMap)) {
      if (coordinate != null && !coordinate.equals(previous)) {
        list.add(coordinate);
        previous = coordinate;
      }
    }
    return GeometryUtils.GEOMETRY_FACTORY_WGS84.createLineString(list.toArray(new Coordinate[0]));
  }

  /** One round trip for a database-backed map, one probe per node for an in-memory one. */
  private static Iterable<Coordinate> coordinates(List<Long> nodes,
      Map<Long, Coordinate> coordinateMap) {
    if (coordinateMap instanceof BatchMap<Long, Coordinate>batch) {
      return batch.getAll(nodes);
    }
    return () -> nodes.stream().map(coordinateMap::get).iterator();
  }
}
