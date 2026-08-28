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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

class VectorTileTest {

  static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  @Test
  void endToEnd() {
    var tile = new Tile(List.of(
        new Layer("layer", 256, List.of(
            new Feature(1, Map.of("a", 1.0, "b", "2"),
                GEOMETRY_FACTORY.createPoint(new Coordinate(1, 2))),
            new Feature(2, Map.of("c", 3.0, "d", "4"),
                GEOMETRY_FACTORY.createPoint(new Coordinate(2, 3)))))));

    var encoded = new VectorTileEncoder().encodeTile(tile);
    var decoded = new VectorTileDecoder().decodeTile(encoded);

    assertEquals(tile, decoded);
  }
}
