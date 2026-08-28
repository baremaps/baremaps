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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.github.benmanes.caffeine.cache.Weigher;
import java.nio.ByteBuffer;
import org.checkerframework.checker.index.qual.NonNegative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A {@code TileStore} decorator that uses caffeine to cache the content of tiles. */
public class VectorTileCache implements TileStore<ByteBuffer> {

  private static final Logger logger = LoggerFactory.getLogger(VectorTileCache.class);

  private final TileStore<ByteBuffer> tileStore;

  private final Cache<TileCoord, ByteBuffer> cache;

  /**
   * Decorates the TileStore with a cache.
   *
   * @param tileStore the tile store
   * @param caffeineSpec the cache specification
   */
  public VectorTileCache(TileStore<ByteBuffer> tileStore, CaffeineSpec caffeineSpec) {
    this.tileStore = tileStore;
    this.cache = Caffeine.from(caffeineSpec).weigher(new Weigher<TileCoord, ByteBuffer>() {
      @Override
      public @NonNegative int weigh(TileCoord tileCoord, ByteBuffer blob) {
        return 28 + blob.capacity();
      }
    }).build();
  }

  /** {@inheritDoc} */
  @Override
  public ByteBuffer read(TileCoord tileCoord) throws TileStoreException {
    var byteBuffer = cache.get(tileCoord, t -> {
      try {
        return tileStore.read(t);
      } catch (TileStoreException e) {
        logger.error("Unable to read the tile.", e);
        return null;
      }
    });
    if (byteBuffer == null) {
      return null;
    } else {
      return byteBuffer.duplicate();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void write(TileCoord tileCoord, ByteBuffer byteBuffer) throws TileStoreException {
    tileStore.write(tileCoord, byteBuffer);
    cache.invalidate(tileCoord);
  }

  /** {@inheritDoc} */
  @Override
  public void delete(TileCoord tileCoord) throws TileStoreException {
    tileStore.delete(tileCoord);
    cache.invalidate(tileCoord);
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws Exception {
    tileStore.close();
    cache.cleanUp();
  }
}
