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

package com.baremaps.openstreetmap.function;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.baremaps.data.collection.BatchMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

class WayGeometryBuilderTest {

  private static final Map<Long, Coordinate> COORDINATES =
      Map.of(1L, new Coordinate(0, 0), 2L, new Coordinate(1, 1), 3L, new Coordinate(2, 0));

  /** A map that counts its batches, as a database-backed map would count its round trips. */
  private static class CountingBatchMap extends HashMap<Long, Coordinate>
      implements BatchMap<Long, Coordinate> {

    int batches;

    CountingBatchMap() {
      super(COORDINATES);
    }

    @Override
    public List<Coordinate> getAll(List<Long> keys) {
      batches++;
      return keys.stream().map(this::get).toList();
    }
  }

  @Test
  void lineStringSkipsMissingNodes() {
    var line = WayGeometryBuilder.lineString(List.of(1L, 2L, 99L, 3L), COORDINATES);
    assertEquals(3, line.getNumPoints());
    assertEquals(new Coordinate(2, 0), line.getCoordinateN(2));
  }

  @Test
  void lineStringReadsABatchMapInOneBatch() {
    var map = new CountingBatchMap();
    var line = WayGeometryBuilder.lineString(List.of(1L, 2L, 99L, 3L), map);
    assertEquals(3, line.getNumPoints());
    assertEquals(1, map.batches);
  }
}
