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

package com.baremaps.openstreetmap.geometry;

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.openstreetmap.function.EntityGeometryBuilder;
import com.baremaps.openstreetmap.model.Info;
import com.baremaps.openstreetmap.model.Member;
import com.baremaps.openstreetmap.model.Member.MemberType;
import com.baremaps.openstreetmap.model.Node;
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.openstreetmap.model.Way;
import com.baremaps.openstreetmap.utils.CRSUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.Proj4jException;
import org.locationtech.proj4j.ProjCoordinate;

class EntityDataColumnTypeGeometryBuilderTest {

  static final CRSFactory CRS_FACTORY = new CRSFactory();

  static final CoordinateReferenceSystem EPSG_4326 = CRSUtils.createFromSrid(4326);

  static final CoordinateTransform COORDINATE_TRANSFORM = new CoordinateTransform() {
    @Override
    public CoordinateReferenceSystem getSourceCRS() {
      return EPSG_4326;
    }

    @Override
    public CoordinateReferenceSystem getTargetCRS() {
      return EPSG_4326;
    }

    @Override
    public ProjCoordinate transform(ProjCoordinate src, ProjCoordinate tgt) throws Proj4jException {
      return src;
    }
  };

  static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

  static final LocalDateTime TIMESTAMP = LocalDateTime.of(2020, 1, 1, 0, 0);

  static final Info INFO = new Info(0, TIMESTAMP, 0, 0);

  static final Node NODE_0 = new Node(0L, INFO, ImmutableMap.of(), 0d, 0d);

  static final Node NODE_1 = new Node(1L, INFO, ImmutableMap.of(), 0d, 4d);

  static final Node NODE_2 = new Node(2L, INFO, ImmutableMap.of(), 4d, 4d);

  static final Node NODE_3 = new Node(3L, INFO, ImmutableMap.of(), 4d, 0d);

  static final Node NODE_4 = new Node(4L, INFO, ImmutableMap.of(), 1d, 1d);

  static final Node NODE_5 = new Node(5L, INFO, ImmutableMap.of(), 1d, 2d);

  static final Node NODE_6 = new Node(6L, INFO, ImmutableMap.of(), 2d, 2d);

  static final Node NODE_7 = new Node(7L, INFO, ImmutableMap.of(), 2d, 1d);

  static final Node NODE_8 = new Node(8L, INFO, ImmutableMap.of(), 4d, 1d);

  static final Node NODE_9 = new Node(9L, INFO, ImmutableMap.of(), 4d, 1d);

  static final Node NODE_10 = new Node(10L, INFO, ImmutableMap.of(), 5d, 2d);

  static final Node NODE_11 = new Node(11L, INFO, ImmutableMap.of(), 5d, 1d);

  static final Node NODE_12 = new Node(12L, INFO, ImmutableMap.of(), 2d, 1d);

  static final Node NODE_13 = new Node(13L, INFO, ImmutableMap.of(), 2d, 2d);

  static final Node NODE_14 = new Node(14L, INFO, ImmutableMap.of(), 3d, 3d);

  static final Node NODE_15 = new Node(15L, INFO, ImmutableMap.of(), 3d, 1d);

  static final Map<Long, Coordinate> COORDINATE_CACHE = new HashMap<>(Arrays
      .asList(NODE_0, NODE_1, NODE_2, NODE_3, NODE_4, NODE_5, NODE_6, NODE_7, NODE_8, NODE_9,
          NODE_10, NODE_11, NODE_12, NODE_13, NODE_14, NODE_15)
      .stream()
      .collect(Collectors.toMap(n -> n.getId(), n -> new Coordinate(n.getLon(), n.getLat()))));

  static final Way WAY_0 = new Way(0L, INFO, ImmutableMap.of(), ImmutableList.of());

  static final Way WAY_1 = new Way(1L, INFO, ImmutableMap.of(), ImmutableList.of(0L, 1L, 2L, 3L));

  static final Way WAY_2 =
      new Way(2L, INFO, ImmutableMap.of(), ImmutableList.of(0L, 1L, 2L, 3L, 0L));

  static final Way WAY_3 =
      new Way(3L, INFO, ImmutableMap.of(), ImmutableList.of(8L, 9L, 10L, 11L, 8L));

  static final Way WAY_4 =
      new Way(4L, INFO, ImmutableMap.of(), ImmutableList.of(4L, 5L, 6L, 7L, 4L));

  static final Way WAY_5 =
      new Way(5L, INFO, ImmutableMap.of(), ImmutableList.of(12L, 13L, 14L, 15L, 12L));

  static final Map<Long, List<Long>> REFERENCE_CACHE =
      new HashMap<>(Arrays.asList(WAY_0, WAY_1, WAY_2, WAY_3, WAY_4, WAY_5).stream()
          .collect(Collectors.toMap(w -> w.getId(), w -> w.getNodes())));

  static final Relation RELATION_0 = new Relation(0L, INFO, ImmutableMap.of(), List.of());

  static final Relation RELATION_1 =
      new Relation(1L, INFO, ImmutableMap.of("type", "multipolygon"), List.of());

  static final Relation RELATION_2 = new Relation(2L, INFO, ImmutableMap.of("type", "multipolygon"),
      List.of(new Member(2L, MemberType.WAY, "outer")));

  static final Relation RELATION_3 =
      new Relation(3L, INFO, ImmutableMap.of("type", "multipolygon"), Arrays.asList(
          new Member(2L, MemberType.WAY, "outer"), new Member(3L, MemberType.WAY, "inner")));

  static final Relation RELATION_4 = new Relation(4L, INFO, ImmutableMap.of("type", "multipolygon"),
      Arrays.asList(new Member(2L, MemberType.WAY, "outer"),
          new Member(3L, MemberType.WAY, "inner"), new Member(4L, MemberType.WAY, "outer")));

  static final Relation RELATION_5 = new Relation(5L, INFO, ImmutableMap.of("type", "multipolygon"),
      Arrays.asList(new Member(2L, MemberType.WAY, "outer"),
          new Member(4L, MemberType.WAY, "inner"), new Member(5L, MemberType.WAY, "inner")));

  static final EntityGeometryBuilder GEOMETRY_BUILDER =
      new EntityGeometryBuilder(COORDINATE_CACHE, REFERENCE_CACHE);

  @Test
  void handleNode() {
    GEOMETRY_BUILDER.accept(NODE_0);
    Point p0 = (Point) NODE_0.getGeometry();
    assertEquals(0, p0.getX());
    assertEquals(0, p0.getY());
    GEOMETRY_BUILDER.accept(NODE_2);
    Point p1 = (Point) NODE_2.getGeometry();
    assertEquals(4, p1.getX());
    assertEquals(4, p1.getY());
  }

  @Test
  void handleWay() {
    GEOMETRY_BUILDER.accept(WAY_0);
    assertNull(WAY_0.getGeometry());
    GEOMETRY_BUILDER.accept(WAY_1);
    assertInstanceOf(LineString.class, WAY_1.getGeometry());
    GEOMETRY_BUILDER.accept(WAY_2);
    assertInstanceOf(Polygon.class, WAY_2.getGeometry());
  }

  @Test
  void handleRelation() {
    GEOMETRY_BUILDER.accept(RELATION_0);
    assertNull(RELATION_0.getGeometry());
    GEOMETRY_BUILDER.accept(RELATION_1);
    assertNotNull(RELATION_1.getGeometry());
    GEOMETRY_BUILDER.accept(RELATION_2);
    assertInstanceOf(MultiPolygon.class, RELATION_2.getGeometry());
    GEOMETRY_BUILDER.accept(RELATION_3);
    assertInstanceOf(MultiPolygon.class, RELATION_3.getGeometry());
    GEOMETRY_BUILDER.accept(RELATION_4);
    assertInstanceOf(MultiPolygon.class, RELATION_4.getGeometry());
  }

  @Test
  void handleRelationWithHole() {
    GEOMETRY_BUILDER.accept(RELATION_5);
    assertInstanceOf(MultiPolygon.class, RELATION_5.getGeometry());
    var multiPolygon = (MultiPolygon) RELATION_5.getGeometry();
    assertEquals(1, multiPolygon.getNumGeometries());
    assertInstanceOf(Polygon.class, multiPolygon.getGeometryN(0));
    var polygon = (Polygon) multiPolygon.getGeometryN(0);
    assertNotNull(polygon.getExteriorRing());
    assertEquals(1, polygon.getNumInteriorRing());
  }
}
