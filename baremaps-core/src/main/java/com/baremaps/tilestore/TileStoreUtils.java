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

import com.baremaps.data.stream.ProgressLogger;
import com.baremaps.data.stream.StreamException;
import com.baremaps.data.stream.StreamUtils;
import org.locationtech.jts.geom.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper methods for moving tiles between tile stores. */
public final class TileStoreUtils {

  private static final Logger logger = LoggerFactory.getLogger(TileStoreUtils.class);

  private TileStoreUtils() {}

  /**
   * Copies every tile of an area to another tile store.
   *
   * <p>
   * Producing a tile is the slow part, whether it is queried from a database or computed from a
   * raster, and several tiles are therefore produced at once. The writes are batched, as a tile
   * store commits a batch in a single round trip.
   *
   * @param source the tile store to read from
   * @param target the tile store to write to
   * @param envelope the area to copy, in longitude and latitude
   * @param minZoom the first zoom level to copy
   * @param maxZoom the last zoom level to copy
   * @param batchSize the number of tiles produced at once, and written per round trip
   * @throws TileStoreException if a tile cannot be read or written
   */
  public static <T> void copy(
      TileStore<T> source,
      TileStore<T> target,
      Envelope envelope,
      int minZoom,
      int maxZoom,
      int batchSize) throws TileStoreException {
    var count = TileCoord.count(envelope, minZoom, maxZoom);
    var start = System.currentTimeMillis();

    var tileCoords = StreamUtils.stream(TileCoord.iterator(envelope, minZoom, maxZoom))
        .peek(new ProgressLogger<>(count, 5000));

    var entries = StreamUtils.bufferInCompletionOrder(tileCoords, tileCoord -> {
      try {
        return new TileEntry<>(tileCoord, source.read(tileCoord));
      } catch (TileStoreException e) {
        throw new StreamException(e);
      }
    }, batchSize);

    try {
      StreamUtils.partition(entries, batchSize).forEach(batch -> {
        try {
          target.write(batch);
        } catch (TileStoreException e) {
          throw new StreamException(e);
        }
      });
    } catch (StreamException e) {
      throw tileStoreException(e);
    }

    var stop = System.currentTimeMillis();
    logger.info("Copied {} tiles in {}s", count, (stop - start) / 1000);
  }

  /**
   * Digs the failure of a tile store out of the unchecked exceptions the stream operations wrap it
   * in, so that the caller of {@link #copy} is told what actually went wrong.
   */
  private static TileStoreException tileStoreException(StreamException exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof TileStoreException tileStoreException) {
        return tileStoreException;
      }
    }
    return new TileStoreException(exception);
  }
}
