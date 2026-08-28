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

package com.baremaps.data.algorithm;

import java.util.Comparator;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * A utility class for computing the Hilbert curve index of geometries. Based on the implementation
 * of the Hilbert curve in the flatgeobuf project (BSD 2-Clause).
 */
public class Hilbert {

  private static final int HILBERT_MAX = (1 << 16) - 1;

  private Hilbert() {
    // Prevent instantiation
  }

  /**
   * A comparator for sorting geometries by their Hilbert curve index.
   */
  public static class HilberComparator implements Comparator<Geometry> {

    private final double minX;
    private final double minY;
    private final double width;
    private final double height;

    /**
     * Constructs a new Hilbert comparator.
     * 
     * @param minX
     * @param minY
     * @param width
     * @param height
     */
    public HilberComparator(double minX, double minY, double width, double height) {
      this.minX = minX;
      this.minY = minY;
      this.width = width;
      this.height = height;
    }

    @Override
    public int compare(Geometry a, Geometry b) {
      long ha = hibert(a.getEnvelopeInternal(), HILBERT_MAX, minX, minY, width, height);
      long hb = hibert(b.getEnvelopeInternal(), HILBERT_MAX, minX, minY, width, height);
      return (ha - hb) > 0 ? 1 : (ha - hb) == 0 ? 0 : -1;
    }
  }

  public static long hibert(Envelope envelope, int hilbertMax, double minX, double minY,
      double width, double height) {
    long x = 0;
    long y = 0;
    if (width != 0.0)
      x = (long) Math
          .floor(hilbertMax * ((envelope.getMinX() + envelope.getMaxX()) / 2 - minX) / width);
    if (height != 0.0)
      y = (long) Math
          .floor(hilbertMax * ((envelope.getMinY() + envelope.getMaxY()) / 2 - minY) / height);
    return hibert(x, y);
  }

  // Based on public domain code at https://github.com/rawrunprotected/hilbert_curves
  private static long hibert(long x, long y) {
    long a = x ^ y;
    long b = 0xFFFF ^ a;
    long c = 0xFFFF ^ (x | y);
    long d = x & (y ^ 0xFFFF);
    long A = a | (b >> 1);
    long B = (a >> 1) ^ a;
    long C = ((c >> 1) ^ (b & (d >> 1))) ^ c;
    long D = ((a & (c >> 1)) ^ (d >> 1)) ^ d;

    a = A;
    b = B;
    c = C;
    d = D;
    A = ((a & (a >> 2)) ^ (b & (b >> 2)));
    B = ((a & (b >> 2)) ^ (b & ((a ^ b) >> 2)));
    C ^= ((a & (c >> 2)) ^ (b & (d >> 2)));
    D ^= ((b & (c >> 2)) ^ ((a ^ b) & (d >> 2)));

    a = A;
    b = B;
    c = C;
    d = D;
    A = ((a & (a >> 4)) ^ (b & (b >> 4)));
    B = ((a & (b >> 4)) ^ (b & ((a ^ b) >> 4)));
    C ^= ((a & (c >> 4)) ^ (b & (d >> 4)));
    D ^= ((b & (c >> 4)) ^ ((a ^ b) & (d >> 4)));

    a = A;
    b = B;
    c = C;
    d = D;
    C ^= ((a & (c >> 8)) ^ (b & (d >> 8)));
    D ^= ((b & (c >> 8)) ^ ((a ^ b) & (d >> 8)));

    a = C ^ (C >> 1);
    b = D ^ (D >> 1);

    long i0 = x ^ y;
    long i1 = b | (0xFFFF ^ (i0 | a));

    i0 = (i0 | (i0 << 8)) & 0x00FF00FF;
    i0 = (i0 | (i0 << 4)) & 0x0F0F0F0F;
    i0 = (i0 | (i0 << 2)) & 0x33333333;
    i0 = (i0 | (i0 << 1)) & 0x55555555;

    i1 = (i1 | (i1 << 8)) & 0x00FF00FF;
    i1 = (i1 | (i1 << 4)) & 0x0F0F0F0F;
    i1 = (i1 | (i1 << 2)) & 0x33333333;
    i1 = (i1 | (i1 << 1)) & 0x55555555;

    long value = ((i1 << 1) | i0);

    return value;
  }
}
