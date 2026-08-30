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

import static com.baremaps.tilestore.TileCoord.max;
import static com.baremaps.tilestore.TileCoord.min;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.locationtech.jts.geom.Envelope;

/**
 * An iterator over the tile coordinates that overlap with an envelope, walking each zoom level row
 * by row before moving up to the next one.
 */
class TileCoordIterator implements Iterator<TileCoord> {

  private final Envelope envelope;

  private final int maxZoom;

  private int z;

  private int x;

  private int y;

  // The bounds of the zoom level being walked, refreshed once per level rather than per tile.
  private int minX;

  private int maxX;

  private int maxY;

  /**
   * Constructs a {@code TileCoordIterator}.
   *
   * @param envelope the envelope
   * @param minZoom the min zoom
   * @param maxZoom the max zoom
   */
  public TileCoordIterator(Envelope envelope, int minZoom, int maxZoom) {
    this.envelope = envelope;
    this.maxZoom = maxZoom;
    enterZoom(minZoom);
  }

  /** Moves to the first tile of a zoom level and caches the bounds of that level. */
  private void enterZoom(int zoom) {
    this.z = zoom;
    if (zoom > maxZoom) {
      return;
    }
    TileCoord min = min(envelope, zoom);
    TileCoord max = max(envelope, zoom);
    this.minX = min.x();
    this.maxX = max.x();
    this.maxY = max.y();
    this.x = min.x();
    this.y = min.y();
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasNext() {
    return z <= maxZoom && x <= maxX && y <= maxY;
  }

  /** {@inheritDoc} */
  @Override
  public TileCoord next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    TileCoord tileCoord = new TileCoord(x, y, z);
    if (x < maxX) {
      x++;
    } else if (y < maxY) {
      y++;
      x = minX;
    } else {
      enterZoom(z + 1);
    }
    return tileCoord;
  }
}
