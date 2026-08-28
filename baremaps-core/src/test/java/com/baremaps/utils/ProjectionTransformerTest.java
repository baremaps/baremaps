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

package com.baremaps.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.baremaps.openstreetmap.function.ProjectionTransformer;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

class ProjectionTransformerTest {

  @Test
  void testPoint() {
    var inputGeom =
        new GeometryFactory(new PrecisionModel(), 4326).createPoint(new Coordinate(1, 1));
    var outputGeom = (Point) new ProjectionTransformer(4326, 3857).transform(inputGeom);
    assertEquals(3857, outputGeom.getSRID());
    assertEquals(111319.49079327357, outputGeom.getX());
    assertEquals(111325.14286638486, outputGeom.getY());
  }
}
