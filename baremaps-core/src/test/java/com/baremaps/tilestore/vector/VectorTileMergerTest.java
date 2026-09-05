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

package com.baremaps.tilestore.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.baremaps.maplibre.vectortile.Feature;
import com.baremaps.maplibre.vectortile.Layer;
import com.baremaps.maplibre.vectortile.Tile;
import com.baremaps.maplibre.vectortile.VectorTileDecoder;
import com.baremaps.maplibre.vectortile.VectorTileEncoder;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.TileStoreException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

/** What a map is made of comes from more than one place, and arrives as one tile. */
class VectorTileMergerTest {

  private static final TileCoord TILE = new TileCoord(1, 1, 1);

  @Test
  void mergesTheLayersOfEveryStore() throws Exception {
    try (var merger = new VectorTileMerger(List.of(store("building"), store("hillshade")))) {
      assertEquals(List.of("building", "hillshade"), names(merger.read(TILE)));
    }
  }

  /** A store with nothing for a tile contributes nothing to it, rather than emptying it. */
  @Test
  void keepsTheTileAStoreHasNothingFor() throws Exception {
    try (var merger = new VectorTileMerger(List.of(store("building"), store(null)))) {
      assertEquals(List.of("building"), names(merger.read(TILE)));
    }
  }

  /** A tile no store has anything for is no tile. */
  @Test
  void readsNothingWhereNoStoreHasAnything() throws Exception {
    try (var merger = new VectorTileMerger(List.of(store(null), store(null)))) {
      assertNull(merger.read(TILE));
    }
  }

  /** A read-only store that answers with one layer of one feature, or with nothing. */
  private static TileStore<ByteBuffer> store(String layer) {
    return new TileStore<ByteBuffer>() {

      @Override
      public ByteBuffer read(TileCoord tileCoord) throws TileStoreException {
        if (layer == null) {
          return null;
        }
        try {
          var point = new GeometryFactory().createPoint(new Coordinate(1, 1));
          var tile = new Tile(List.of(new Layer(layer, 4096,
              List.of(new Feature(1, Map.of("key", "value"), point)))));
          var bytes = new ByteArrayOutputStream();
          try (var gzip = new GZIPOutputStream(bytes)) {
            new VectorTileEncoder().encodeTile(tile).writeTo(gzip);
          }
          return ByteBuffer.wrap(bytes.toByteArray());
        } catch (Exception e) {
          throw new TileStoreException(e);
        }
      }

      @Override
      public void write(TileCoord tileCoord, ByteBuffer blob) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void delete(TileCoord tileCoord) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void close() {
        // Nothing to close.
      }
    };
  }

  /** The layers of a merged tile, which is gzipped as the tile protocol expects. */
  private static List<String> names(ByteBuffer blob) throws Exception {
    var bytes = new byte[blob.remaining()];
    blob.get(bytes);
    try (var gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
      return new VectorTileDecoder().decodeTile(ByteBuffer.wrap(gzip.readAllBytes()))
          .getLayers().stream().map(Layer::getName).toList();
    }
  }
}
