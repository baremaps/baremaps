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

package com.baremaps.tilestore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;

class TileCoordTest {

  @Test
  void getTile() {
    double lon = 0, lat = 0;
    for (int z = 0; z <= 14; z += 1) {
      TileCoord tileCoord = TileCoord.fromLonLat(lon, lat, z);
      int value = (int) Math.pow(2, z - 1);
      assertEquals(value, tileCoord.x());
      assertEquals(value, tileCoord.y());
    }
  }

  @Test
  void count() {
    Envelope envelope = new TileCoord(0, 0, 0).envelope();
    assertEquals(TileCoord.count(envelope, 0, 2), TileCoord.list(envelope, 0, 2).size());
  }

  @Test
  void countLiechtenstein() {
    double minLon = 9.471078;
    double maxLon = 9.636217;
    double minLat = 47.04774;
    double maxLat = 47.27128;
    int minZoom = 12;
    int maxZoom = 14;
    Envelope envelope = new Envelope(minLon, maxLon, minLat, maxLat);
    assertEquals(TileCoord.count(envelope, minZoom, maxZoom),
        TileCoord.list(envelope, minZoom, maxZoom).size());
  }
}
