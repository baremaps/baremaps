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

/** An iterator over the tile coordinates that overlaps with an envelope. */
class TileCoordIterator implements Iterator<TileCoord> {

  private final Envelope envelope;

  private final int minZoom;

  private final int maxZoom;

  private int z;

  private int x;

  private int y;

  /**
   * Constructs a {@code TileCoordIterator}.
   *
   * @param envelope the envelope
   * @param minZoom the min zoom
   * @param maxZoom the max zoom
   */
  public TileCoordIterator(Envelope envelope, int minZoom, int maxZoom) {
    this.envelope = envelope;
    this.minZoom = minZoom;
    this.maxZoom = maxZoom;

    this.z = this.minZoom;
    TileCoord min = min(envelope, this.z);
    this.x = min.x();
    this.y = min.y();
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasNext() {
    TileCoord max = max(envelope, this.z);
    return x <= max.x() && y <= max.y() && z <= maxZoom;
  }

  /** {@inheritDoc} */
  @Override
  public TileCoord next() {
    TileCoord tileCoord = new TileCoord(x, y, z);
    TileCoord max = max(envelope, this.z);
    if (z > max.z()) {
      throw new NoSuchElementException();
    }
    if (x < max.x()) {
      x++;
    } else if (y < max.y()) {
      y++;
      TileCoord min = min(envelope, this.z);
      x = min.x();
    } else {
      z++;
      TileCoord min = min(envelope, this.z);
      x = min.x();
      y = min.y();
    }
    return tileCoord;
  }
}
