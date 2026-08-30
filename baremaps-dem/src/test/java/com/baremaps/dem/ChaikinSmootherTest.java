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

package com.baremaps.dem;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

class ChaikinSmootherTest {

  @Test
  void smoothLineString() {
    LineString lineString = new GeometryFactory().createLineString(new Coordinate[] {
        new Coordinate(0, 0),
        new Coordinate(1, 1),
    });
    Geometry smoothedLineString = new ChaikinSmoother(2, 0.25).transform(lineString);
    assertEquals(
        "LINESTRING (0 0, 0.0625 0.0625, 0.1875 0.1875, 0.375 0.375, 0.625 0.625, "
            + "0.8125 0.8125, 0.9375 0.9375, 1 1)",
        smoothedLineString.toString());
  }

  @Test
  void smoothRing() {
    Geometry ring = new GeometryFactory().createPolygon(new Coordinate[] {
        new Coordinate(0, 0),
        new Coordinate(0, 4),
        new Coordinate(4, 4),
        new Coordinate(4, 0),
        new Coordinate(0, 0),
    });
    Geometry smoothedRing = new ChaikinSmoother(1, 0.25).transform(ring);
    assertEquals(
        "POLYGON ((0 1, 0 3, 1 4, 3 4, 4 3, 4 1, 3 0, 1 0, 0 1))",
        smoothedRing.toString());
  }

  @Test
  void rejectFactorOutOfRange() {
    assertThrows(IllegalArgumentException.class, () -> new ChaikinSmoother(2, 0));
    assertThrows(IllegalArgumentException.class, () -> new ChaikinSmoother(2, 0.75));
  }
}
