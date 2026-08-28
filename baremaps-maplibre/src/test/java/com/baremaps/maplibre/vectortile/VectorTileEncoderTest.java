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

package com.baremaps.maplibre.vectortile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;

class VectorTileEncoderTest {

  private final GeometryFactory geometryFactory = new GeometryFactory();

  /**
   * An example encoding of a point located at:
   *
   * (25,17)
   */
  @Test
  void encodePoint() {
    var coordinate = new Coordinate(25, 17);
    var point = geometryFactory.createPoint(coordinate);
    var encoder = new VectorTileEncoder();
    var encoding = new ArrayList<Integer>();
    encoder.encodePoint(point, encoding::add);
    assertEquals(List.of(9, 50, 34), encoding);
  }

  /**
   * 4.3.5.2. Example Multi Point
   *
   * An example encoding of two points located at:
   *
   * (5,7) (3,2)
   */
  @Test
  void encodeMultiPoint() {
    var coordinates = new Coordinate[] {
        new Coordinate(5, 7),
        new Coordinate(3, 2)
    };
    var multiPoint = geometryFactory.createMultiPoint(coordinates);
    var encoder = new VectorTileEncoder();
    var encoding = new ArrayList<Integer>();
    encoder.encodeMultiPoint(multiPoint, encoding::add);
    assertEquals(List.of(17, 10, 14, 3, 9), encoding);
  }

  /**
   * 4.3.5.3. Example Linestring
   *
   * An example encoding of a line with the points:
   *
   * (2,2) (2,10) (10,10)
   */
  @Test
  void encodeLineString() {
    var lineString = geometryFactory.createLineString(new Coordinate[] {
        new Coordinate(2, 2),
        new Coordinate(2, 10),
        new Coordinate(10, 10)
    });
    var encoder = new VectorTileEncoder();
    var encoding = new ArrayList<Integer>();
    encoder.encodeLineString(lineString, encoding::add);
    assertEquals(List.of(9, 4, 4, 18, 0, 16, 16, 0), encoding);
  }

  /**
   * 4.3.5.4. Example Multi Linestring
   * <p>
   * An example encoding of two lines with the points:
   * <p>
   * Line 1: - (2,2) - (2,10) - (10,10) Line 2: - (1,1) - (3,5)
   */
  @Test
  void encodeMultiLineString() {
    var lineString1 = geometryFactory.createLineString(new Coordinate[] {
        new Coordinate(2, 2),
        new Coordinate(2, 10),
        new Coordinate(10, 10)
    });;
    var lineString2 = geometryFactory.createLineString(new Coordinate[] {
        new Coordinate(1, 1),
        new Coordinate(3, 5)
    });
    var multiLineString =
        geometryFactory.createMultiLineString(new LineString[] {lineString1, lineString2});
    var encoder = new VectorTileEncoder();
    var encoding = new ArrayList<Integer>();
    encoder.encodeMultiLineString(multiLineString, encoding::add);
    assertEquals(List.of(9, 4, 4, 18, 0, 16, 16, 0, 9, 17, 17, 10, 4, 8), encoding);
  }

  /**
   * 4.3.5.5. Example Polygon
   * <p>
   * An example encoding of a polygon feature that has the points:
   * <p>
   * (3,6) (8,12) (20,34) (3,6) Path Closing as Last Point
   */
  @Test
  void encodePolygon() {
    var polygon = geometryFactory.createPolygon(new Coordinate[] {
        new Coordinate(3, 6),
        new Coordinate(8, 12),
        new Coordinate(20, 34),
        new Coordinate(3, 6)
    });
    var encoder = new VectorTileEncoder();
    var encoding = new ArrayList<Integer>();
    encoder.encodePolygon(polygon, encoding::add);
    assertEquals(List.of(9, 6, 12, 18, 10, 12, 24, 44, 15), encoding);
  }

  /**
   * 4.3.5.6. Example Multi Polygon An example of a more complex encoding of two polygons, one with
   * a hole. The position of the points for the polygons are shown below. The winding order of the
   * polygons is VERY important in this example as it signifies the difference between interior
   * rings and a new polygon.
   * <p>
   * Polygon 1: Exterior Ring: (0,0) (10,0) (10,10) (0,10) (0,0) Path Closing as Last Point Polygon
   * 2: Exterior Ring: (11,11) (20,11) (20,20) (11,20) (11,11) Path Closing as Last Point Interior
   * Ring: (13,13) (13,17) (17,17) (17,13) (13,13) Path Closing as Last Point
   */
  @Test
  void encodeMultiPolygon() {
    var multiPolygon = geometryFactory.createMultiPolygon(
        new Polygon[] {
            geometryFactory.createPolygon(
                new Coordinate[] {
                    new Coordinate(0, 0),
                    new Coordinate(10, 0),
                    new Coordinate(10, 10),
                    new Coordinate(0, 10),
                    new Coordinate(0, 0)
                }),
            geometryFactory.createPolygon(
                geometryFactory.createLinearRing(
                    new Coordinate[] {
                        new Coordinate(11, 11),
                        new Coordinate(20, 11),
                        new Coordinate(20, 20),
                        new Coordinate(11, 20),
                        new Coordinate(11, 11)
                    }),
                new LinearRing[] {
                    geometryFactory.createLinearRing(
                        new Coordinate[] {
                            new Coordinate(13, 13),
                            new Coordinate(13, 17),
                            new Coordinate(17, 17),
                            new Coordinate(17, 13),
                            new Coordinate(13, 13)
                        })
                })
        });
    var encoder = new VectorTileEncoder();
    var encoding = new ArrayList<Integer>();
    encoder.encodeMultiPolygon(multiPolygon, encoding::add);
    assertEquals(List.of(9, 0, 0, 26, 20, 0, 0, 20, 19, 0, 15, 9, 22, 2, 26, 18, 0, 0, 18, 17, 0,
        15, 9, 4, 13, 26, 0, 8, 8, 0, 0, 7, 15), encoding);
  }
}
