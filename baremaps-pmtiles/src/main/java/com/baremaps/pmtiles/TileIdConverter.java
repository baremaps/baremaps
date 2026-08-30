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

package com.baremaps.pmtiles;

import java.util.Arrays;

/**
 * Converts between tile coordinates and the tile ids that order a PMTiles archive.
 * <p>
 * Tiles are numbered zoom level by zoom level, and within a level along a Hilbert curve. The curve
 * keeps neighbouring tiles close in the ordering, so a viewport maps to few contiguous ranges of
 * the archive.
 */
final class TileIdConverter {

  /**
   * The highest zoom level a tile id fits in: the ids of zoom 27 would exceed the range the
   * specification reserves for them.
   */
  private static final int MAX_ZOOM = 26;

  /**
   * The number of tiles at every zoom level below the index, that is {@code (4^z - 1) / 3}. It is
   * tabulated rather than computed so that the zoom level of a tile id can be found by search.
   */
  private static final long[] ZOOM_OFFSETS = {
      0, 1, 5, 21, 85, 341, 1365, 5461, 21845, 87381, 349525, 1398101, 5592405,
      22369621, 89478485, 357913941, 1431655765, 5726623061L, 22906492245L,
      91625968981L, 366503875925L, 1466015503701L, 5864062014805L, 23456248059221L,
      93824992236885L, 375299968947541L, 1501199875790165L,
  };

  private TileIdConverter() {
    // Static utility.
  }

  /**
   * Converts tile coordinates to a tile id.
   *
   * @param z the zoom level
   * @param x the column
   * @param y the row
   * @return the tile id
   * @throws IllegalArgumentException if the coordinates fall outside the zoom level
   */
  static long zxyToTileId(int z, long x, long y) {
    if (z < 0 || z > MAX_ZOOM) {
      throw new IllegalArgumentException("Tile zoom level outside the range 0-" + MAX_ZOOM);
    }
    var size = 1L << z;
    if (x < 0 || y < 0 || x >= size || y >= size) {
      throw new IllegalArgumentException("Tile x/y outside zoom level bounds");
    }

    var position = 0L;
    var point = new Point(x, y);
    for (var span = size / 2; span > 0; span /= 2) {
      var rx = (point.x() & span) > 0 ? 1L : 0L;
      var ry = (point.y() & span) > 0 ? 1L : 0L;
      position += span * span * ((3 * rx) ^ ry);
      point = rotate(span, point, rx, ry);
    }
    return ZOOM_OFFSETS[z] + position;
  }

  /**
   * Converts a tile id back to tile coordinates.
   *
   * @param tileId the tile id
   * @return the coordinates of the tile
   * @throws IllegalArgumentException if the tile id addresses no tile
   */
  static TileCoord tileIdToZxy(long tileId) {
    var index = Arrays.binarySearch(ZOOM_OFFSETS, tileId);
    var z = index >= 0 ? index : -index - 2;
    if (z < 0 || z > MAX_ZOOM || tileId - ZOOM_OFFSETS[z] >= (1L << z) * (1L << z)) {
      throw new IllegalArgumentException("Tile id outside the range of zoom levels 0-" + MAX_ZOOM);
    }

    var position = tileId - ZOOM_OFFSETS[z];
    var size = 1L << z;
    var point = new Point(0, 0);
    for (var span = 1L; span < size; span *= 2) {
      var rx = 1 & (position / 2);
      var ry = 1 & (position ^ rx);
      point = rotate(span, point, rx, ry);
      point = new Point(point.x() + span * rx, point.y() + span * ry);
      position /= 4;
    }
    return new TileCoord(z, point.x(), point.y());
  }

  /**
   * Reflects a point within its quadrant, the step that turns the four quadrants of a level into a
   * single continuous Hilbert curve.
   */
  private static Point rotate(long span, Point point, long rx, long ry) {
    if (ry != 0) {
      return point;
    }
    var reflected = rx == 1 ? new Point(span - 1 - point.x(), span - 1 - point.y()) : point;
    return new Point(reflected.y(), reflected.x());
  }

  private record Point(long x, long y) {
  }
}
