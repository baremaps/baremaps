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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;

class TileStoreUtilsTest {

  private static final Envelope WORLD = new Envelope(-180, 180, -85.0511, 85.0511);

  @Test
  void copyEveryTileOfTheZoomLevels() throws TileStoreException {
    var source = new MapTileStore();
    var target = new MapTileStore();

    TileStoreUtils.copy(source, target, WORLD, 0, 2, 4);

    assertEquals(TileCoord.count(WORLD, 0, 2), target.tiles.size());
    for (var tileCoord : TileCoord.list(WORLD, 0, 2)) {
      assertEquals(content(tileCoord), target.tiles.get(tileCoord));
    }
  }

  @Test
  void reportTheFailureOfTheSource() {
    var source = new MapTileStore();
    source.failing = true;

    assertThrows(TileStoreException.class,
        () -> TileStoreUtils.copy(source, new MapTileStore(), WORLD, 0, 1, 4));
  }

  private static ByteBuffer content(TileCoord tileCoord) {
    return ByteBuffer.wrap(tileCoord.toString().getBytes(StandardCharsets.UTF_8));
  }

  /** A tile store that answers with the coordinate of the tile, and remembers what it is given. */
  private static class MapTileStore implements TileStore<ByteBuffer> {

    private final Map<TileCoord, ByteBuffer> tiles = new ConcurrentHashMap<>();

    private boolean failing;

    @Override
    public ByteBuffer read(TileCoord tileCoord) throws TileStoreException {
      if (failing) {
        throw new TileStoreException("Cannot read the tile");
      }
      return content(tileCoord);
    }

    @Override
    public void write(TileCoord tileCoord, ByteBuffer blob) {
      tiles.put(tileCoord, blob);
    }

    @Override
    public void delete(TileCoord tileCoord) {
      tiles.remove(tileCoord);
    }

    @Override
    public void close() {
      // Nothing to close
    }
  }
}
