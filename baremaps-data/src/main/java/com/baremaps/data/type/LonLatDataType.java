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

package com.baremaps.data.type;

import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;

import java.lang.foreign.MemorySegment;
import org.locationtech.jts.geom.Coordinate;

/**
 * A {@link DataType} for reading and writing longitude/latitude coordinates in
 * {@link MemorySegment}s. An integer is used to compress the coordinates to the detriment of
 * precision (centimeters).
 */
public class LonLatDataType extends FixedSizeDataType<Coordinate> {

  // Each axis gets a 32-bit lane; scaling by 2^31 rather than 2^32 keeps the inclusive upper
  // bounds (180 and 90) from overflowing the lane, at the price of one bit of precision.
  private static final double BITS = Math.pow(2, 31);
  private static final long SHIFT = 32;
  private static final long MASK = (1L << 32) - 1L;

  /**
   * Constructs a {@link LonLatDataType} with a fixed size of {@link Long#BYTES}.
   */
  public LonLatDataType() {
    super(Long.BYTES);
  }

  /**
   * Encodes longitude and latitude values into a single long value.
   *
   * @param lon the longitude value
   * @param lat the latitude value
   * @return the encoded coordinate as a long value
   */
  public static long encodeLonLat(final double lon, final double lat) {
    long x = (long) (((lon + 180) / 360) * BITS);
    long y = (long) (((lat + 90) / 180) * BITS);
    long l = (x << SHIFT);
    long r = (y & MASK);
    return l | r;
  }

  /**
   * Decodes the longitude value from an encoded coordinate.
   *
   * @param value the encoded coordinate
   * @return the longitude value
   */
  public static double decodeLon(final long value) {
    double l = (value >>> 32);
    return (l / BITS) * 360 - 180;
  }

  /**
   * Decodes the latitude value from an encoded coordinate.
   *
   * @param value the encoded coordinate
   * @return the latitude value
   */
  public static double decodeLat(final long value) {
    long r = (value & MASK);
    return (r / BITS) * 180 - 90;
  }

  /** {@inheritDoc} */
  @Override
  public void write(final MemorySegment segment, final long position, final Coordinate value) {
    segment.set(JAVA_LONG_UNALIGNED, position, encodeLonLat(value.x, value.y));
  }

  /** {@inheritDoc} */
  @Override
  public Coordinate read(final MemorySegment segment, final long position) {
    var value = segment.get(JAVA_LONG_UNALIGNED, position);
    return new Coordinate(decodeLon(value), decodeLat(value));
  }
}
