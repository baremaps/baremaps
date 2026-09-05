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

import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.TileStoreException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A {@code TileStore} that serves the layers of several stores as one tile.
 *
 * <p>
 * What a map is made of does not all come from one place: the roads are queried from a database and
 * the relief is traced from an elevation archive. They are still one map, and a browser that reads
 * them as two sources fetches every place twice, holds two tile pyramids that can disagree about
 * which zoom levels exist, and has to be told in the style which half each layer belongs to.
 * Merging them here means the map has one source and the layers no longer say where they came from.
 *
 * <p>
 * The merge is a concatenation, because a vector tile is a sequence of layers and nothing else: the
 * format has no header to reconcile and no offsets to fix up, so appending the layers of one tile
 * to another is appending the bytes. What the stores have to agree on is the names, since two
 * layers of one name are two layers a client reads one of. A store with nothing for a coordinate,
 * such as the terrain outside the zoom levels it is traced at, contributes nothing.
 *
 * <p>
 * The tiles are handed over compressed, as the tile protocol expects, so this decompresses what it
 * is given and compresses the result once. That is one pass more than a tile assembled in a single
 * place would cost, and it is paid on the server rather than by every reader of the map.
 */
public class VectorTileMerger implements TileStore<ByteBuffer> {

  private final List<TileStore<ByteBuffer>> tileStores;

  /**
   * Constructs a {@code VectorTileMerger} over the stores whose layers make up a tile.
   *
   * @param tileStores the stores, in the order their layers are written
   */
  public VectorTileMerger(List<TileStore<ByteBuffer>> tileStores) {
    this.tileStores = tileStores;
  }

  /** {@inheritDoc} */
  @Override
  public ByteBuffer read(TileCoord tileCoord) throws TileStoreException {
    try (var tile = new ByteArrayOutputStream()) {
      var empty = true;
      for (var tileStore : tileStores) {
        var blob = tileStore.read(tileCoord);
        if (blob == null) {
          continue;
        }
        empty = false;
        decompress(blob, tile);
      }
      if (empty) {
        return null;
      }
      return compress(tile.toByteArray());
    } catch (IOException e) {
      throw new TileStoreException(e);
    }
  }

  private static void decompress(ByteBuffer blob, ByteArrayOutputStream tile) throws IOException {
    var bytes = new byte[blob.remaining()];
    blob.get(bytes);
    try (var gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
      gzip.transferTo(tile);
    }
  }

  private static ByteBuffer compress(byte[] tile) throws IOException {
    try (var bytes = new ByteArrayOutputStream()) {
      try (var gzip = new GZIPOutputStream(bytes)) {
        gzip.write(tile);
      }
      return ByteBuffer.wrap(bytes.toByteArray());
    }
  }

  /** The tiles are merged as they are read, so there is nothing to write to. */
  @Override
  public void write(TileCoord tileCoord, ByteBuffer blob) throws TileStoreException {
    throw new UnsupportedOperationException("The merged tile store is read only");
  }

  /** The tiles are merged as they are read, so there is nothing to delete from. */
  @Override
  public void delete(TileCoord tileCoord) throws TileStoreException {
    throw new UnsupportedOperationException("The merged tile store is read only");
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws Exception {
    for (var tileStore : tileStores) {
      tileStore.close();
    }
  }
}
