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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;

class VectorTileFunctionsTest {

  @Test
  void vectorTileGeom() {
    // Create a test geometry (a simple square)
    var coordinates = new Coordinate[] {
        new Coordinate(1, 1),
        new Coordinate(5, 9),
        new Coordinate(9, 1),
        new Coordinate(1, 1)
    };
    var geometryFactory = new GeometryFactory();
    var inputGeom = geometryFactory.createPolygon(coordinates);

    // Define the tile envelope, extent, buffer, and clipping flag
    var envelope = geometryFactory.toGeometry(new Envelope(0, 10, 0, 10));
    var extent = 100;
    var buffer = 10;
    var clipGeom = true;

    // Transform the input geometry into a vector tile geometry
    var outputGeom =
        VectorTileFunctions.asVectorTileGeom(inputGeom, envelope, extent, buffer, clipGeom);

    // Check if the output geometry is not null
    assertNotNull(outputGeom);

    // Check if the output geometry is a valid Geometry
    assertTrue(outputGeom.isValid());

    // Define the expected coordinates for the transformed geometry
    Coordinate[] expectedCoordinates = new Coordinate[] {
        new Coordinate(10, 90),
        new Coordinate(90, 90),
        new Coordinate(50, 10),
        new Coordinate(10, 90)
    };

    // Compare the transformed geometry with the expected geometry
    LinearRing expectedShell = geometryFactory.createLinearRing(expectedCoordinates);
    Polygon expectedGeom = geometryFactory.createPolygon(expectedShell);
    assertTrue(outputGeom.equalsTopo(expectedGeom));

    // Transform back the vector tile geometry into the original geometry
    var backToInputGeom = VectorTileFunctions.fromVectorTileGeom(outputGeom, envelope, extent);
    assertTrue(backToInputGeom.equalsTopo(inputGeom));
  }
}
