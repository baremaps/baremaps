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

package com.baremaps.testing;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

/**
 * Utility methods for comparing geometries.
 */
public class GeometryAssertions {

  private GeometryAssertions() {
    // Prevent instantiation
  }

  private static void throwAssertionError(Object expected, Object actual) {
    throw new AssertionError("Expected " + expected + " but was " + actual);
  }

  /**
   * Asserts that two geometries are equal.
   *
   * @param expected the expected wkt geometry
   * @param actual the actual wkt geometry
   */
  public static void assertGeometryEquals(String expected, String actual) {
    try {
      var reader = new WKTReader();
      assertGeometryEquals(reader.read(expected), reader.read(expected));
    } catch (Exception e) {
      throwAssertionError(expected, actual);
    }
  }

  /**
   * Asserts that two geometries are equal.
   *
   * @param expected the expected wkt geometry
   * @param actual the actual jts geometry
   */
  public static void assertGeometryEquals(String expected, Geometry actual) {
    try {
      assertGeometryEquals(new WKTReader().read(expected), actual);
    } catch (Exception e) {
      throwAssertionError(expected, actual);
    }
  }

  /**
   * Asserts that two geometries are equal.
   *
   * @param expected the expected jts geometry
   * @param actual the actual jts geometry
   */
  public static void assertGeometryEquals(Geometry expected, Geometry actual) {
    if (expected == null && actual == null) {
      return;
    }
    if (expected == null || actual == null) {
      throwAssertionError(expected, actual);
    }
    if (!expected.equals(actual)) {
      throwAssertionError(expected, actual);
    }
  }

  /**
   * Asserts that two geometries are approximately equal within a tolerance.
   *
   * @param expected the expected jts geometry
   * @param actual the actual jts geometry
   * @param tolerance the tolerance factor
   */
  public static void assertGeometryEquals(Geometry expected, Geometry actual, double tolerance) {
    if (expected == null && actual == null) {
      return;
    }
    if (expected == null || actual == null) {
      throwAssertionError(expected, actual);
    }
    PrecisionModel precisionModel = new PrecisionModel(tolerance);
    GeometryPrecisionReducer reducer = new GeometryPrecisionReducer(precisionModel);
    reducer.setPointwise(true);
    assertGeometryEquals(reducer.reduce(expected), reducer.reduce(actual));
  }
}
