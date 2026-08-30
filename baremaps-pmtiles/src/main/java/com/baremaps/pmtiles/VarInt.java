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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Reads and writes the LEB128 variable-length integers that encode every number in a PMTiles
 * directory.
 * <p>
 * The reference JavaScript implementation splits each value into a low and a high half because its
 * numbers are 53-bit doubles. A Java {@code long} holds the full range, so the halves are merged
 * back into a single loop here.
 */
final class VarInt {

  /** The largest number of bytes a 64-bit value can occupy: ceil(64 / 7). */
  private static final int MAX_BYTES = 10;

  private VarInt() {
    // Static utility.
  }

  /**
   * Writes a value as a variable-length integer.
   *
   * @param output the stream to write to
   * @param value the value to write
   * @throws IOException if an I/O error occurs
   */
  static void write(OutputStream output, long value) throws IOException {
    while ((value & ~0x7fL) != 0) {
      output.write((int) (value & 0x7f) | 0x80);
      value >>>= 7;
    }
    output.write((int) value);
  }

  /**
   * Reads a variable-length integer.
   *
   * @param input the stream to read from
   * @return the value that was read
   * @throws IOException if the stream ends mid-value, or the value is not a valid varint
   */
  static long read(InputStream input) throws IOException {
    long value = 0;
    for (var shift = 0; shift < MAX_BYTES * 7; shift += 7) {
      var b = input.read();
      if (b < 0) {
        throw new EOFException("Truncated varint");
      }
      value |= (long) (b & 0x7f) << shift;
      if (b < 0x80) {
        return value;
      }
    }
    throw new IOException("Varint longer than " + MAX_BYTES + " bytes");
  }
}
