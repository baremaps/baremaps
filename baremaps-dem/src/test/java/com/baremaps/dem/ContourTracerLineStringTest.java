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
import org.locationtech.jts.geom.LineString;

class ContourTracerLineStringTest {

  @Test
  @DisplayName("Test case 0")
  void testProcessCellWithCase00() {
    var lines = trace(MarchingSquareUtils.CASE_00);
    assertTrue(lines.isEmpty());
  }

  @Test
  @DisplayName("Test case 1")
  void testProcessCellWithCase01() {
    var lines = trace(MarchingSquareUtils.CASE_01);
    assertGeometryEquals("LINESTRING (0 0.5, 0.5 0)", lines.get(0));
  }

  @Test
  @DisplayName("Test case 2")
  void testProcessCellWithCase02() {
    var lines = trace(MarchingSquareUtils.CASE_02);
    assertEquals(1, lines.size());
    assertGeometryEquals("LINESTRING (0.5 0, 1 0.5)", lines.get(0));
  }

  @Test
  @DisplayName("Test case 3")
  void testProcessCellWithCase03() {
    var lines = trace(MarchingSquareUtils.CASE_03);
    assertEquals(1, lines.size());
    assertGeometryEquals("LINESTRING (0 0.5, 1 0.5)", lines.get(0));
  }

  @Test
  @DisplayName("Test case 4")
  void testProcessCellWithCase04() {
    var lines = trace(MarchingSquareUtils.CASE_04);
    assertEquals(1, lines.size());
    assertGeometryEquals("LINESTRING (1 0.5, 0.5 1)", lines.get(0));
  }

  @Test
  @DisplayName("Test case 5")
  void testProcessCellWithCase05() {
    var lines = trace(MarchingSquareUtils.CASE_05);
    assertEquals(2, lines.size());
    assertGeometryEquals("LINESTRING (0 0.5, 0.5 1)", lines.get(0));
    assertGeometryEquals("LINESTRING (1 0.5, 0.5 0)", lines.get(1));
  }

  @Test
  @DisplayName("Test case 6")
  void testProcessCellWithCase06() {
    var lines = trace(MarchingSquareUtils.CASE_06);
    assertEquals(1, lines.size());
    assertGeometryEquals("LINESTRING (0.5 0, 0.5 1)", lines.get(0));
  }

  @Test
  @DisplayName("Test case 7")
  void testProcessCellWithCase07() {
    var lines = trace(MarchingSquareUtils.CASE_07);
    assertEquals(1, lines.size());
    assertGeometryEquals("LINESTRING (0 0.5, 0.5 1)", lines.get(0));
  }

  @Test
  @DisplayName("Test case 8")
  void testProcessCellWithCase08() {
    var lines = trace(MarchingSquareUtils.CASE_08);
    assertEquals(1, lines.size());
    assertGeometryEquals("LINESTRING (0.5 1, 0 0.5)", lines.get(0));
  }

  @Test
  @DisplayName("Test case 9")
  void testProcessCellWithCase09() {
    var lines = trace(MarchingSquareUtils.CASE_09);
    assertEquals(1, lines.size());
    assertGeometryEquals("LINESTRING (0.5 1, 0.5 0)", lines.get(0));
  }

  @Test
  @DisplayName("Test case 10")
  void testProcessCellWithCase10() {
    var lines = trace(MarchingSquareUtils.CASE_10);
    assertEquals(2, lines.size());
    assertGeometryEquals("LINESTRING (0.5 0, 0 0.5)", lines.get(0));
    assertGeometryEquals("LINESTRING (0.5 1, 1 0.5)", lines.get(1));
  }

  @Test
  @DisplayName("Test case 11")
  void testProcessCellWithCase11() {
    var lines = trace(MarchingSquareUtils.CASE_11);
    assertEquals(1, lines.size());
    assertGeometryEquals("LINESTRING (0.5 1, 1 0.5)", lines.get(0));
  }

  @Test
  @DisplayName("Test case 12")
  void testProcessCellWithCase12() {
    var lines = trace(MarchingSquareUtils.CASE_12);
    assertEquals(1, lines.size());
    assertGeometryEquals("LINESTRING (1 0.5, 0 0.5)", lines.get(0));
  }

  @Test
  @DisplayName("Test case 13")
  void testProcessCellWithCase13() {
    var lines = trace(MarchingSquareUtils.CASE_13);
    assertEquals(1, lines.size());
    assertGeometryEquals("LINESTRING (1 0.5, 0.5 0)", lines.get(0));
  }

  @Test
  @DisplayName("Test case 14")
  void testProcessCellWithCase14() {
    var lines = trace(MarchingSquareUtils.CASE_14);
    assertEquals(1, lines.size());
    assertGeometryEquals("LINESTRING (0.5 0, 0 0.5)", lines.get(0));
  }

  @Test
  @DisplayName("Test case 15")
  void testProcessCellWithCase15() {
    var lines = trace(MarchingSquareUtils.CASE_15);
    assertTrue(lines.isEmpty());
  }

  List<LineString> trace(double[] grid) {
    return new ContourTracer(grid, 2, 2).traceLines(0.5);
  }

}
