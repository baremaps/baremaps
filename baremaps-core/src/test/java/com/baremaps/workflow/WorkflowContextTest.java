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

package com.baremaps.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;

/** The maps of the cache directory survive being closed, which is what an update relies on. */
class WorkflowContextTest {

  @Test
  void mapsAreReopenedFromTheCache(@TempDir Path dir) throws Exception {
    var context = new WorkflowContext(dir.resolve("data"), dir.resolve("cache"));
    try (var coordinates = context.getCoordinateMap();
        var references = context.getReferenceMap()) {
      assertTrue(coordinates.isEmpty());
      coordinates.put(10L, new Coordinate(6.6, 46.5));
      coordinates.put(3_000_000_000L, new Coordinate(-73.9, 40.7));
      references.put(20L, List.of(10L, 3_000_000_000L));
    }
    try (var coordinates = context.getCoordinateMap();
        var references = context.getReferenceMap()) {
      assertEquals(2, coordinates.size());
      assertEquals(6.6, coordinates.get(10L).x, 1e-6);
      assertEquals(40.7, coordinates.get(3_000_000_000L).y, 1e-6);
      assertNull(coordinates.get(11L));
      assertEquals(List.of(10L, 3_000_000_000L), references.get(20L));
      // An update adds keys in any order, and out of order of the import.
      coordinates.put(5L, new Coordinate(0, 0));
      assertEquals(3, coordinates.size());
    }
    context.cleanCache();
    try (var coordinates = context.getCoordinateMap()) {
      assertTrue(coordinates.isEmpty());
    }
  }
}
