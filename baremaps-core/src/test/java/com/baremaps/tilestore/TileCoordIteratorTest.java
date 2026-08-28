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

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;

class TileCoordIteratorTest {

  @Test
  void iterator() {
    Envelope e = new TileCoord(0, 0, 0).envelope();

    TileCoordIterator i0 = new TileCoordIterator(e, 0, 0);
    List<TileCoord> l0 = ImmutableList.copyOf(i0);
    assertEquals(1, l0.size());

    TileCoordIterator i1 = new TileCoordIterator(e, 1, 1);
    List<TileCoord> l1 = ImmutableList.copyOf(i1);
    assertEquals(4, l1.size());

    TileCoordIterator i2 = new TileCoordIterator(e, 2, 2);
    List<TileCoord> l2 = ImmutableList.copyOf(i2);
    assertEquals(16, l2.size());
  }

  @Test
  void partial() {
    Envelope e0 = new TileCoord(0, 0, 1).envelope();
    TileCoordIterator i0 = new TileCoordIterator(e0, 2, 2);
    List<TileCoord> l0 = ImmutableList.copyOf(i0);
    assertEquals(4, l0.size());

    Envelope e1 = new TileCoord(1, 1, 1).envelope();
    TileCoordIterator i1 = new TileCoordIterator(e1, 2, 2);
    List<TileCoord> l1 = ImmutableList.copyOf(i1);
    assertEquals(4, l1.size());
  }
}
