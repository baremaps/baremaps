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
import com.baremaps.openstreetmap.model.Member;
import com.baremaps.openstreetmap.model.Member.MemberType;
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.openstreetmap.utils.GeometryUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.geom.util.PolygonExtracter;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A consumer that builds and sets a relation geometry via side effects.
 */
public class RelationMultiPolygonBuilder implements Consumer<Entity> {

  private static final Logger logger = LoggerFactory.getLogger(RelationMultiPolygonBuilder.class);

  private final Map<Long, Coordinate> coordinateMap;
  private final Map<Long, List<Long>> referenceMap;

  /**
   * Constructs a relation geometry builder.
   *
   * @param coordinateMap the coordinates map
   * @param referenceMap the references map
   */
  public RelationMultiPolygonBuilder(
      Map<Long, Coordinate> coordinateMap,
      Map<Long, List<Long>> referenceMap) {
    this.coordinateMap = coordinateMap;
    this.referenceMap = referenceMap;
  }

  /** {@inheritDoc} */
  @Override
  public void accept(Entity entity) {
    if (entity instanceof Relation relation) {
      try {
        var start = System.currentTimeMillis();

        buildMultiPolygon(relation);

        var end = System.currentTimeMillis();
        var duration = end - start;
        if (duration > 60 * 1000) {
          logger.debug("Relation #{} processed in {} ms", relation.getId(), duration);
        }

      } catch (Exception e) {
        logger.debug("Unable to build the geometry for relation #" + relation.getId(), e);
        var emptyMultiPolygon = GeometryUtils.GEOMETRY_FACTORY_WGS84.createMultiPolygon();
        relation.setGeometry(emptyMultiPolygon);
      }
    }
  }

  @SuppressWarnings("squid:S3776")
  private void buildMultiPolygon(Relation relation) {
    // Categorize the members of the relation by their role
    var outerMembers = new ArrayList<Member>();
    var innerMembers = new ArrayList<Member>();
    var otherMembers = new ArrayList<Member>();
    for (var member : relation.getMembers()) {
      if (MemberType.WAY.equals(member.type())) {
        switch (member.role()) {
          case "outer" -> outerMembers.add(member);
          case "inner" -> innerMembers.add(member);
          default -> otherMembers.add(member);
        }
      }
    }

    // Prepare the outer polygons
    var outerPolygons = createPolygons(outerMembers);

    // Prepare the inner polygons
    var innerPolygons = createPolygons(innerMembers);
    innerPolygons = combinePolygons(innerPolygons);

    // Prepare the other polygons
    var otherPolygons = createPolygons(otherMembers);
    otherPolygons = combinePolygons(otherPolygons);

    // Categorize the other polygons as inner or outer
    for (var otherPolygon : otherPolygons) {
      // A polygon contained in an outer polygon is a hole, any other one is an outer polygon
      var contained = outerPolygons.stream().anyMatch(outer -> outer.contains(otherPolygon));
      (contained ? innerPolygons : outerPolygons).add(otherPolygon);
    }

    // Merge the outer and inner polygons to build the relation geometry
    var polygons = new ArrayList<Polygon>();

    // Iterate over the outer polygons
    for (var outerPolygon : outerPolygons) {

      // Initialize the shell and holes of the polygon
      var shell = outerPolygon.getExteriorRing();
      var holes = new ArrayList<LinearRing>();
      for (int i = 0; i < outerPolygon.getNumInteriorRing(); i++) {
        holes.add(outerPolygon.getInteriorRingN(i));
      }

      // Use a prepared geometry to speed up the contains operation
      var preparedOuterPolygon = PreparedGeometryFactory.prepare(outerPolygon);

      // Find to which inner polygon the outer polygon belongs
      for (var innerPolygon : innerPolygons) {
        if (preparedOuterPolygon.contains(innerPolygon)) {

          // The exterior ring of the inner polygon is a hole in the outer polygon
          holes.add(innerPolygon.getExteriorRing());

          // The interior rings of the inner polygon are in fact additional polygons
          for (int i = 0; i < innerPolygon.getNumInteriorRing(); i++) {
            var innerPolygonHole =
                GeometryUtils.GEOMETRY_FACTORY_WGS84
                    .createPolygon(innerPolygon.getInteriorRingN(i));
            repairPolygon(innerPolygonHole, polygons);
          }
        }
      }

      // Build the polygon from the shell and holes
      var polygon = GeometryUtils.GEOMETRY_FACTORY_WGS84.createPolygon(shell,
          holes.toArray(new LinearRing[0]));
      repairPolygon(polygon, polygons);
    }

    // Build the multipolygon from the polygons
    if (!polygons.isEmpty()) {
      var multiPolygon =
          GeometryUtils.GEOMETRY_FACTORY_WGS84.createMultiPolygon(polygons.toArray(new Polygon[0]));
      relation.setGeometry(multiPolygon);
    } else {
      var emptyMultiPolygon = GeometryUtils.GEOMETRY_FACTORY_WGS84.createMultiPolygon();
      relation.setGeometry(emptyMultiPolygon);
    }
  }

  private List<Polygon> createPolygons(List<Member> members) {
    var polygons = new ArrayList<Polygon>();
    var lineMerger = new LineMerger();

    // A member can either be a polygon or a line part of a polygon
    for (Member member : members) {
      var lineString = createLineString(member);
      if (lineString.isClosed()) {
        var polygon =
            GeometryUtils.GEOMETRY_FACTORY_WGS84.createPolygon(lineString.getCoordinateSequence());
        repairPolygon(polygon, polygons);
      } else {
        lineMerger.add(lineString);
      }
    }

    // Merge the lines to build the polygons
    for (Object geometry : lineMerger.getMergedLineStrings()) {
      if (geometry instanceof LineString lineString && lineString.isClosed()) {
        var polygon =
            GeometryUtils.GEOMETRY_FACTORY_WGS84.createPolygon(lineString.getCoordinates());
        repairPolygon(polygon, polygons);
      }
    }

    return polygons;
  }

  private LineString createLineString(Member member) {
    return WayGeometryBuilder.lineString(referenceMap.get(member.ref()), coordinateMap);
  }

  private List<Polygon> combinePolygons(List<Polygon> polygons) {
    // Members of the same role may touch, overlap or nest (e.g. an island inside a lake inside an
    // outer ring). Folding them with symDifference applies even-odd semantics, so nested polygons
    // become holes and overlapping ones merge, and yields a topologically valid result.
    var geometry = GeometryUtils.GEOMETRY_FACTORY_WGS84.createEmpty(0);
    for (Polygon polygon : polygons) {
      geometry = geometry.symDifference(polygon);
    }
    var combinedPolygons = new ArrayList<Polygon>();
    PolygonExtracter.getPolygons(geometry, combinedPolygons);
    return combinedPolygons;
  }

  private void repairPolygon(Polygon polygon, List<Polygon> accumulator) {
    if (polygon.isValid()) {
      accumulator.add(polygon);
    } else {
      var geometryFixer = new GeometryFixer(polygon);
      var fixedGeometry = geometryFixer.getResult();
      if (fixedGeometry instanceof Polygon fixedPolygon) {
        accumulator.add(fixedPolygon);
      } else if (fixedGeometry instanceof MultiPolygon fixedMultiPolygon) {
        PolygonExtracter.getPolygons(fixedMultiPolygon, accumulator);
      }
    }
  }
}
