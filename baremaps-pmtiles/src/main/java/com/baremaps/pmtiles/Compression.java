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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The compression algorithms a PMTiles archive can apply to its directories, its metadata and its
 * tiles.
 * <p>
 * <strong>The ordinal of each constant is the value stored in the header.</strong> Reordering,
 * inserting or removing a constant silently changes how every existing archive is interpreted, so
 * new algorithms may only be appended.
 */
public enum Compression {

  UNKNOWN,
  NONE,
  GZIP,
  BROTLI,
  ZSTD;

  /**
   * Wraps a stream so that it decompresses with this algorithm.
   *
   * @param input the compressed stream
   * @return a stream yielding the decompressed bytes
   * @throws IOException if an I/O error occurs
   */
  public InputStream decompress(InputStream input) throws IOException {
    return switch (this) {
      case NONE -> input;
      case GZIP -> new GZIPInputStream(input);
      default -> throw new UnsupportedOperationException(this + " decompression not implemented");
    };
  }

  /**
   * Wraps a stream so that it compresses with this algorithm.
   * <p>
   * The returned stream must be closed for the compressed data to be complete.
   *
   * @param output the stream to write the compressed bytes to
   * @return a stream accepting the uncompressed bytes
   * @throws IOException if an I/O error occurs
   */
  OutputStream compress(OutputStream output) throws IOException {
    return switch (this) {
      case NONE -> output;
      case GZIP -> new GZIPOutputStream(output);
      default -> throw new UnsupportedOperationException(this + " compression not implemented");
    };
  }

  /**
   * Returns the compression stored under the given header value.
   *
   * @param value the value read from the header
   * @return the corresponding compression
   * @throws IOException if the value is not a known compression
   */
  static Compression forHeaderValue(int value) throws IOException {
    var values = values();
    if (value < 0 || value >= values.length) {
      throw new IOException("Unknown compression value: " + value);
    }
    return values[value];
  }
}
