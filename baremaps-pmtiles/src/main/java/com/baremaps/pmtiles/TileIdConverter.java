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

import com.google.common.math.LongMath;

/**
 * Utility to convert between tile coordinates and tileIds.
 */
class TileIdConverter {

  private static final long[] TZ_VALUES = new long[] {
      0, 1, 5, 21, 85, 341, 1365, 5461, 21845, 87381, 349525, 1398101, 5592405,
      22369621, 89478485, 357913941, 1431655765, 5726623061L, 22906492245L,
      91625968981L, 366503875925L, 1466015503701L, 5864062014805L, 23456248059221L,
      93824992236885L, 375299968947541L, 1501199875790165L,
  };

  /**
   * Rotate coordinates using Hilbert curve rotation.
   *
   * @param n the size of the quadrant
   * @param xy the coordinates to rotate
   * @param rx the x transform
   * @param ry the y transform
   */
  private static void rotate(long n, long[] xy, long rx, long ry) {
    if (ry == 0) {
      if (rx == 1) {
        xy[0] = n - 1 - xy[0];
        xy[1] = n - 1 - xy[1];
      }
      long t = xy[0];
      xy[0] = xy[1];
      xy[1] = t;
    }
  }

  /**
   * Convert a position to z, x, y coordinates.
   *
   * @param z the zoom level
   * @param pos the position
   * @return the z, x, y coordinates
   */
  public static long[] idOnLevel(int z, long pos) {
    long n = LongMath.pow(2, z);
    long rx, ry, t = pos;
    long[] xy = new long[] {0, 0};
    long s = 1;
    while (s < n) {
      rx = 1 & (t / 2);
      ry = 1 & (t ^ rx);
      rotate(s, xy, rx, ry);
      xy[0] += s * rx;
      xy[1] += s * ry;
      t = t / 4;
      s *= 2;
    }
    return new long[] {z, xy[0], xy[1]};
  }

  /**
   * Convert z, x, y coordinates to a tileId.
   *
   * @param z the zoom level
   * @param x the x coordinate
   * @param y the y coordinate
   * @return the tileId
   */
  public static long zxyToTileId(int z, long x, long y) {
    if (z > 26) {
      throw new IllegalArgumentException("Tile zoom level exceeds max safe number limit (26)");
    }
    if (x > Math.pow(2, z) - 1 || y > Math.pow(2, z) - 1) {
      throw new IllegalArgumentException("tile x/y outside zoom level bounds");
    }
    long acc = TZ_VALUES[z];
    long n = LongMath.pow(2, z);
    long rx = 0;
    long ry = 0;
    long d = 0;
    long[] xy = new long[] {x, y};
    long s = n / 2;
    while (s > 0) {
      rx = (xy[0] & s) > 0 ? 1 : 0;
      ry = (xy[1] & s) > 0 ? 1 : 0;
      d += s * s * ((3 * rx) ^ ry);
      rotate(s, xy, rx, ry);
      s = s / 2;
    }
    return acc + d;
  }

  /**
   * Convert a tileId to z, x, y coordinates.
   *
   * @param i the tileId
   * @return the z, x, y coordinates
   */
  public static long[] tileIdToZxy(long i) {
    long acc = 0;
    for (int z = 0; z < 27; z++) {
      long numTiles = (0x1L << z) * (0x1L << z);
      if (acc + numTiles > i) {
        return idOnLevel(z, i - acc);
      }
      acc += numTiles;
    }
    throw new IllegalArgumentException("Tile zoom level exceeds max safe number limit (26)");
  }
}
