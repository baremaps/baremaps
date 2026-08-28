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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Serializer for variable-length integers used in PMTiles format.
 */
class VarIntSerializer {

  /**
   * Constructs a new VarIntSerializer.
   */
  VarIntSerializer() {
    // Empty constructor
  }

  /**
   * Combine low and high bits into a single number.
   *
   * @param low the low 32 bits
   * @param high the high 32 bits
   * @return the combined 64-bit number
   */
  private long toNum(long low, long high) {
    return high * 0x100000000L + low;
  }

  /**
   * Read variable integer remainder from input stream.
   *
   * @param input the input stream
   * @param l the low bits
   * @return the read varint
   * @throws IOException if an I/O error occurs
   */
  private long readVarIntRemainder(InputStream input, long l) throws IOException {
    long h, b;
    b = input.read() & 0xff;
    h = (b & 0x70) >> 4;
    if (b < 0x80) {
      return toNum(l, h);
    }
    b = input.read() & 0xff;
    h |= (b & 0x7f) << 3;
    if (b < 0x80) {
      return toNum(l, h);
    }
    b = input.read() & 0xff;
    h |= (b & 0x7f) << 10;
    if (b < 0x80) {
      return toNum(l, h);
    }
    b = input.read() & 0xff;
    h |= (b & 0x7f) << 17;
    if (b < 0x80) {
      return toNum(l, h);
    }
    b = input.read() & 0xff;
    h |= (b & 0x7f) << 24;
    if (b < 0x80) {
      return toNum(l, h);
    }
    b = input.read() & 0xff;
    h |= (b & 0x01) << 31;
    if (b < 0x80) {
      return toNum(l, h);
    }
    throw new IllegalArgumentException("Expected varint not more than 10 bytes");
  }

  /**
   * Write a variable-length integer to the output stream.
   *
   * @param output the output stream
   * @param value the value to write
   * @return the number of bytes written
   * @throws IOException if an I/O error occurs
   */
  public int writeVarInt(OutputStream output, long value) throws IOException {
    int n = 1;
    while (value >= 0x80) {
      output.write((byte) (value | 0x80));
      value >>>= 7;
      n++;
    }
    output.write((byte) value);
    return n;
  }

  /**
   * Read a variable-length integer from the input stream.
   *
   * @param input the input stream
   * @return the read varint
   * @throws IOException if an I/O error occurs
   */
  public long readVarInt(InputStream input) throws IOException {
    long val, b;
    b = input.read() & 0xff;
    val = b & 0x7f;
    if (b < 0x80) {
      return val;
    }
    b = input.read() & 0xff;
    val |= (b & 0x7f) << 7;
    if (b < 0x80) {
      return val;
    }
    b = input.read() & 0xff;
    val |= (b & 0x7f) << 14;
    if (b < 0x80) {
      return val;
    }
    b = input.read() & 0xff;
    val |= (b & 0x7f) << 21;
    if (b < 0x80) {
      return val;
    }
    val |= (b & 0x0f) << 28;
    return readVarIntRemainder(input, val);
  }
}
