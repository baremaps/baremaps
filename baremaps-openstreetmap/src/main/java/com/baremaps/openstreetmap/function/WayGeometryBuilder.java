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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A consumer that builds and sets a way geometry via side effects.
 */
public class WayGeometryBuilder implements Consumer<Entity> {

  private static final Logger logger = LoggerFactory.getLogger(WayGeometryBuilder.class);

  private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

  private final Map<Long, Coordinate> coordinateMap;

  /**
   * Constructs a way geometry builder.
   *
   * @param coordinateMap the coordinates map
   */
  public WayGeometryBuilder(Map<Long, Coordinate> coordinateMap) {
    this.coordinateMap = coordinateMap;
  }

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("squid:S3776")
  public void accept(Entity entity) {
    if (entity instanceof Way way) {
      try {
        // Build the coordinate list and remove duplicates.
        List<Coordinate> list = new ArrayList<>();
        Coordinate previous = null;
        for (Long id : way.getNodes()) {
          Coordinate coordinate = coordinateMap.get(id);
          if (coordinate != null && !coordinate.equals(previous)) {
            list.add(coordinate);
            previous = coordinate;
          }
        }

        Coordinate[] array = list.toArray(new Coordinate[0]);
        LineString line = geometryFactory.createLineString(array);

        if (!line.isEmpty()) {
          // Ways can be open or closed depending on the geometry or the tags:
          // https://wiki.openstreetmap.org/wiki/Way
          if (!line.isClosed()
              || way.getTags().containsKey("railway")
              || way.getTags().containsKey("highway")
              || way.getTags().containsKey("barrier")) {
            way.setGeometry(line);
          } else {
            Polygon polygon = geometryFactory.createPolygon(line.getCoordinates());
            if (polygon.isValid()) {
              way.setGeometry(polygon);
            } else {
              var geometryFixer = new GeometryFixer(polygon);
              var fixedGeometry = geometryFixer.getResult();
              way.setGeometry(fixedGeometry);
            }
          }
        }
      } catch (Exception e) {
        logger.debug("Unable to build the geometry for way #" + way.getId(), e);
        way.setGeometry(geometryFactory.createEmpty(0));
      }
    }
  }
}
