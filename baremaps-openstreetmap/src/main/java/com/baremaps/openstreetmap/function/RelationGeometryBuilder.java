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

import static com.baremaps.data.geometry.GeometryUtils.GEOMETRY_FACTORY_WGS84;

import com.baremaps.openstreetmap.model.Member;
import com.baremaps.openstreetmap.model.Member.MemberType;
import com.baremaps.openstreetmap.model.Relation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.geom.util.PolygonExtracter;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A consumer that sets the multipolygon geometry of the relations it accepts.
 *
 * <p>
 * A relation is assembled from the ways it references, which the relation only names: the rings
 * they form have to be rebuilt from the node references recorded earlier in the file.
 */
public class RelationGeometryBuilder implements Consumer<Relation> {

  private static final Logger logger = LoggerFactory.getLogger(RelationGeometryBuilder.class);

  private final Map<Long, Coordinate> coordinateMap;
  private final Map<Long, List<Long>> referenceMap;

  /**
   * Constructs a relation geometry builder.
   *
   * @param coordinateMap the coordinates of the nodes seen so far
   * @param referenceMap the node ids of the ways seen so far
   */
  public RelationGeometryBuilder(
      Map<Long, Coordinate> coordinateMap,
      Map<Long, List<Long>> referenceMap) {
    this.coordinateMap = coordinateMap;
    this.referenceMap = referenceMap;
  }

  /**
   * Sets the geometry of the relation, leaving it unset when the relation does not describe an
   * area. A relation whose geometry cannot be built gets an empty multipolygon rather than none, so
   * that a consumer can tell "could not be built" from "does not have one".
   */
  @Override
  public void accept(Relation relation) {
    if (!isArea(relation)) {
      return;
    }
    try {
      relation.setGeometry(buildMultiPolygon(relation));
    } catch (Exception e) {
      logger.debug("Unable to build the geometry of relation #{}", relation.getId(), e);
      relation.setGeometry(GEOMETRY_FACTORY_WGS84.createMultiPolygon());
    }
  }

  /**
   * Returns true if the relation describes an area. Only multipolygons and boundaries do:
   * boundaries are administrative areas whose rings are shared with neighbours, and other relation
   * types (routes, restrictions, ...) have no single geometry. Coastline relations are excluded
   * because the coastline of a landmass is too large to assemble in memory.
   */
  private static boolean isArea(Relation relation) {
    var tags = relation.getTags();
    if ("coastline".equals(tags.get("natural"))) {
      return false;
    }
    return "multipolygon".equals(tags.get("type")) || "boundary".equals(tags.get("type"));
  }

  private MultiPolygon buildMultiPolygon(Relation relation) {
    var outerPolygons = ringsOf(relation, "outer");
    var innerPolygons = combine(ringsOf(relation, "inner"));

    // A member with no role, or an unknown one, is a hole when it falls inside an outer ring and an
    // outer ring otherwise. Deciding by containment is what the role would otherwise have told us.
    for (var polygon : combine(ringsOf(relation, null))) {
      var contained = outerPolygons.stream().anyMatch(outer -> outer.contains(polygon));
      (contained ? innerPolygons : outerPolygons).add(polygon);
    }

    var polygons = new ArrayList<Polygon>();
    for (var outerPolygon : outerPolygons) {
      addWithHoles(outerPolygon, innerPolygons, polygons);
    }
    return GEOMETRY_FACTORY_WGS84.createMultiPolygon(polygons.toArray(new Polygon[0]));
  }

  /**
   * Builds the polygons formed by the way members holding the given role, or by those holding
   * neither "outer" nor "inner" when the role is null. A member is either a closed way, hence a
   * ring on its own, or a fragment that only closes once merged with the other fragments.
   */
  private List<Polygon> ringsOf(Relation relation, String role) {
    var polygons = new ArrayList<Polygon>();
    var lineMerger = new LineMerger();
    for (Member member : relation.getMembers()) {
      if (!MemberType.WAY.equals(member.type()) || !hasRole(member, role)) {
        continue;
      }
      var lineString = lineStringOf(member);
      if (lineString.isClosed()) {
        addRepaired(GEOMETRY_FACTORY_WGS84.createPolygon(lineString.getCoordinateSequence()),
            polygons);
      } else {
        lineMerger.add(lineString);
      }
    }
    for (Object geometry : lineMerger.getMergedLineStrings()) {
      if (geometry instanceof LineString lineString && lineString.isClosed()) {
        addRepaired(GEOMETRY_FACTORY_WGS84.createPolygon(lineString.getCoordinates()), polygons);
      }
    }
    return polygons;
  }

  private static boolean hasRole(Member member, String role) {
    if (role != null) {
      return role.equals(member.role());
    }
    return !"outer".equals(member.role()) && !"inner".equals(member.role());
  }

  private LineString lineStringOf(Member member) {
    var references = referenceMap.get(member.ref());
    if (references == null) {
      // The way is not in the file, or appeared after the relation that references it.
      return GEOMETRY_FACTORY_WGS84.createLineString();
    }
    return WayGeometryBuilder.lineString(references, coordinateMap);
  }

  /**
   * Adds the outer polygon to the accumulator, punched with the inner polygons it contains.
   *
   * <p>
   * An inner polygon may itself have holes, and a hole in a hole is land again: an island in a lake
   * is part of the relation, so those rings are emitted as polygons of their own.
   */
  private void addWithHoles(Polygon outerPolygon, List<Polygon> innerPolygons,
      List<Polygon> accumulator) {
    var holes = new ArrayList<LinearRing>();
    for (int i = 0; i < outerPolygon.getNumInteriorRing(); i++) {
      holes.add(outerPolygon.getInteriorRingN(i));
    }
    // Containment is tested against every inner polygon, so the outer ring is indexed once.
    var preparedOuterPolygon = PreparedGeometryFactory.prepare(outerPolygon);
    for (var innerPolygon : innerPolygons) {
      if (preparedOuterPolygon.contains(innerPolygon)) {
        holes.add(innerPolygon.getExteriorRing());
        for (int i = 0; i < innerPolygon.getNumInteriorRing(); i++) {
          addRepaired(GEOMETRY_FACTORY_WGS84.createPolygon(innerPolygon.getInteriorRingN(i)),
              accumulator);
        }
      }
    }
    addRepaired(GEOMETRY_FACTORY_WGS84.createPolygon(outerPolygon.getExteriorRing(),
        holes.toArray(new LinearRing[0])), accumulator);
  }

  /**
   * Merges polygons that share a role into a set of disjoint ones.
   *
   * <p>
   * Members of the same role may touch, overlap or nest (e.g. an island inside a lake inside an
   * outer ring). Folding them with symDifference applies even-odd semantics, so nested polygons
   * become holes and overlapping ones merge, and yields a topologically valid result.
   */
  private static List<Polygon> combine(List<Polygon> polygons) {
    if (polygons.size() < 2) {
      return polygons;
    }
    Geometry geometry = polygons.get(0);
    for (int i = 1; i < polygons.size(); i++) {
      geometry = geometry.symDifference(polygons.get(i));
    }
    var combinedPolygons = new ArrayList<Polygon>();
    PolygonExtracter.getPolygons(geometry, combinedPolygons);
    return combinedPolygons;
  }

  /**
   * Adds the polygon to the accumulator, repairing it first if it is invalid. Repairing can split a
   * self-intersecting ring into several polygons, and can also fail, in which case the ring is
   * dropped rather than allowed to corrupt the result.
   */
  private static void addRepaired(Polygon polygon, List<Polygon> accumulator) {
    if (polygon.isValid()) {
      accumulator.add(polygon);
      return;
    }
    var fixedGeometry = new GeometryFixer(polygon).getResult();
    if (fixedGeometry instanceof Polygon fixedPolygon) {
      accumulator.add(fixedPolygon);
    } else if (fixedGeometry instanceof MultiPolygon fixedMultiPolygon) {
      PolygonExtracter.getPolygons(fixedMultiPolygon, accumulator);
    }
  }
}
