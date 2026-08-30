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

import static com.baremaps.testing.GeometryAssertions.assertGeometryEquals;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

class ContourTracerPolygonTest {

  @Test
  @DisplayName("Test case 0")
  void testProcessCellWithCase00() {
    var polygons = trace(MarchingSquareUtils.CASE_00);
    assertTrue(polygons.isEmpty());
  }

  @Test
  @DisplayName("Test case 1")
  void testProcessCellWithCase01() {
    var polygons = trace(MarchingSquareUtils.CASE_01);
    assertGeometryEquals("POLYGON ((0.5 0, 0 0, 0 0.5, 0.5 0))", polygons.get(0));
  }

  @Test
  @DisplayName("Test case 2")
  void testProcessCellWithCase02() {
    var polygons = trace(MarchingSquareUtils.CASE_02);
    assertEquals(1, polygons.size());
    assertGeometryEquals("POLYGON ((1 0.5, 1 0, 0.5 0, 1 0.5))", polygons.get(0));
  }

  @Test
  @DisplayName("Test case 3")
  void testProcessCellWithCase03() {
    var polygons = trace(MarchingSquareUtils.CASE_03);
    assertEquals(1, polygons.size());
    assertGeometryEquals("POLYGON ((0 0, 0 0.5, 1 0.5, 1 0, 0 0))", polygons.get(0));
  }

  @Test
  @DisplayName("Test case 4")
  void testProcessCellWithCase04() {
    var polygons = trace(MarchingSquareUtils.CASE_04);
    assertEquals(1, polygons.size());
    assertGeometryEquals("POLYGON ((1 1, 1 0.5, 0.5 1, 1 1))", polygons.get(0));
  }

  @Test
  @DisplayName("Test case 5")
  void testProcessCellWithCase05() {
    var polygons = trace(MarchingSquareUtils.CASE_05);
    assertEquals(1, polygons.size());
    assertGeometryEquals("POLYGON ((1 1, 1 0.5, 0.5 0, 0 0, 0 0.5, 0.5 1, 1 1))",
        polygons.get(0));
  }

  @Test
  @DisplayName("Test case 6")
  void testProcessCellWithCase06() {
    var polygons = trace(MarchingSquareUtils.CASE_06);
    assertEquals(1, polygons.size());
    assertGeometryEquals("POLYGON ((0.5 1, 1 1, 1 0, 0.5 0, 0.5 1))", polygons.get(0));
  }

  @Test
  @DisplayName("Test case 7")
  void testProcessCellWithCase07() {
    var polygons = trace(MarchingSquareUtils.CASE_07);
    assertEquals(1, polygons.size());
    assertGeometryEquals("POLYGON ((1 1, 1 0, 0 0, 0 0.5, 0.5 1, 1 1))", polygons.get(0));
  }

  @Test
  @DisplayName("Test case 8")
  void testProcessCellWithCase08() {
    var polygons = trace(MarchingSquareUtils.CASE_08);
    assertEquals(1, polygons.size());
    assertGeometryEquals("POLYGON ((0.5 1, 0 0.5, 0 1, 0.5 1))", polygons.get(0));
  }

  @Test
  @DisplayName("Test case 9")
  void testProcessCellWithCase09() {
    var polygons = trace(MarchingSquareUtils.CASE_09);
    assertEquals(1, polygons.size());
    assertGeometryEquals("POLYGON ((0.5 1, 0.5 0, 0 0, 0 1, 0.5 1))", polygons.get(0));
  }

  @Test
  @DisplayName("Test case 10")
  void testProcessCellWithCase10() {
    var polygons = trace(MarchingSquareUtils.CASE_10);
    assertEquals(1, polygons.size());
    assertGeometryEquals("POLYGON ((0.5 1, 1 0.5, 1 0, 0.5 0, 0 0.5, 0 1, 0.5 1))",
        polygons.get(0));
  }

  @Test
  @DisplayName("Test case 11")
  void testProcessCellWithCase11() {
    var polygons = trace(MarchingSquareUtils.CASE_11);
    assertEquals(1, polygons.size());
    assertGeometryEquals("POLYGON ((1 0, 0 0, 0 1, 0.5 1, 1 0.5, 1 0))", polygons.get(0));
  }

  @Test
  @DisplayName("Test case 12")
  void testProcessCellWithCase12() {
    var polygons = trace(MarchingSquareUtils.CASE_12);
    assertEquals(1, polygons.size());
    assertGeometryEquals("POLYGON ((1 0.5, 0 0.5, 0 1, 1 1, 1 0.5))", polygons.get(0));
  }

  @Test
  @DisplayName("Test case 13")
  void testProcessCellWithCase13() {
    var polygons = trace(MarchingSquareUtils.CASE_13);
    assertEquals(1, polygons.size());
    assertGeometryEquals("POLYGON ((0.5 0, 0 0, 0 1, 1 1, 1 0.5, 0.5 0))", polygons.get(0));
  }

  @Test
  @DisplayName("Test case 14")
  void testProcessCellWithCase14() {
    var polygons = trace(MarchingSquareUtils.CASE_14);
    assertEquals(1, polygons.size());
    assertGeometryEquals("POLYGON ((0 0.5, 0 1, 1 1, 1 0, 0.5 0, 0 0.5))", polygons.get(0));
  }

  @Test
  @DisplayName("Test case 15")
  void testProcessCellWithCase15() {
    var polygons = trace(MarchingSquareUtils.CASE_15);
    assertEquals(1, polygons.size());
    assertGeometryEquals("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))", polygons.get(0));
  }

  public static List<Polygon> trace(double[] grid) {
    int size = (int) Math.sqrt(grid.length);
    return new ContourTracer(grid, size, size).tracePolygons(0.5);
  }

  @Test
  @DisplayName("Closed LineString Bug 1")
  void testProcessCellWithCaseClosedLineStringBug1() {
    var grid = new double[] {
        133.3801854240864, 121.23325422569911, 153.52819487296264, 167.57377406292332,
        100.937349262252, 96.62896517856514, 132.65708268451803, 147.307915356104,
        98.7659308960914, 104.55934526711907, 141.3071569592299, 151.71211629894563,
        139.87179431281479, 144.5483699607932, 171.74295402684095, 175.7901414451639
    };
    var contours = new ContourTracer(grid, 4, 4).tracePolygons(99);
    assertFalse(contours.isEmpty());
    assertTrue(contours.stream().allMatch(Polygon.class::isInstance));
  }

  @Test
  @DisplayName("Closed LineString Bug 2")
  void testProcessCellWithCaseClosedLineStringBug2() {
    var grid = new double[] {
        491.0, 495.0, 502.0, 503.0,
        487.0, 493.0, 500.0, 499.0,
        490.0, 495.0, 500.0, 497.0,
        499.0, 502.0, 505.0, 500.0,
    };
    var contours = new ContourTracer(grid, 4, 4).tracePolygons(500);
    assertFalse(contours.isEmpty());
    assertTrue(contours.stream().allMatch(Polygon.class::isInstance));
  }
}
