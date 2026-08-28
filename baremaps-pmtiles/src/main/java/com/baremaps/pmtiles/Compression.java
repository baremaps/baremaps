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

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Enumeration of compression algorithms supported by PMTiles. Provides methods to compress and
 * decompress data streams.
 */
public enum Compression {
  UNKNOWN,
  NONE,
  GZIP,
  BROTLI,
  ZSTD;

  /**
   * Decompresses an input stream using the compression algorithm represented by this enum value.
   *
   * @param inputStream the input stream to decompress
   * @return a new input stream that decompresses the given stream
   * @throws IOException if an I/O error occurs
   */
  InputStream decompress(InputStream inputStream) throws IOException {
    return switch (this) {
      case NONE -> inputStream;
      case GZIP -> decompressGzip(inputStream);
      case BROTLI -> decompressBrotli(inputStream);
      case ZSTD -> decompressZstd(inputStream);
      default -> throw new UnsupportedOperationException("Unknown compression format");
    };
  }

  /**
   * Decompresses an input stream using GZIP.
   *
   * @param inputStream the input stream to decompress
   * @return a new GZIP input stream
   * @throws IOException if an I/O error occurs
   */
  static InputStream decompressGzip(InputStream inputStream) throws IOException {
    return new GZIPInputStream(inputStream);
  }

  /**
   * Decompresses an input stream using Brotli.
   *
   * @param buffer the input stream to decompress
   * @return a new Brotli input stream
   * @throws UnsupportedOperationException as Brotli is not yet implemented
   */
  static InputStream decompressBrotli(InputStream buffer) {
    throw new UnsupportedOperationException("Brotli compression not implemented");
  }

  /**
   * Decompresses an input stream using Zstandard.
   *
   * @param buffer the input stream to decompress
   * @return a new Zstandard input stream
   * @throws UnsupportedOperationException as Zstandard is not yet implemented
   */
  static InputStream decompressZstd(InputStream buffer) {
    throw new UnsupportedOperationException("Zstd compression not implemented");
  }

  /**
   * Compresses an output stream using the compression algorithm represented by this enum value.
   *
   * @param outputStream the output stream to compress
   * @return a new output stream that compresses to the given stream
   * @throws IOException if an I/O error occurs
   */
  OutputStream compress(OutputStream outputStream) throws IOException {
    return switch (this) {
      case NONE -> outputStream;
      case GZIP -> compressGzip(outputStream);
      case BROTLI -> compressBrotli(outputStream);
      case ZSTD -> compressZstd(outputStream);
      default -> throw new UnsupportedOperationException("Unknown compression format");
    };
  }

  /**
   * Compresses an output stream using GZIP.
   *
   * @param outputStream the output stream to compress
   * @return a new GZIP output stream
   * @throws IOException if an I/O error occurs
   */
  static OutputStream compressGzip(OutputStream outputStream) throws IOException {
    return new GZIPOutputStream(outputStream);
  }

  /**
   * Compresses an output stream using Brotli.
   *
   * @param outputStream the output stream to compress
   * @return a new Brotli output stream
   * @throws UnsupportedOperationException as Brotli is not yet implemented
   */
  static OutputStream compressBrotli(OutputStream outputStream) {
    throw new UnsupportedOperationException("Brotli compression not implemented");
  }

  /**
   * Compresses an output stream using Zstandard.
   *
   * @param outputStream the output stream to compress
   * @return a new Zstandard output stream
   * @throws UnsupportedOperationException as Zstandard is not yet implemented
   */
  static OutputStream compressZstd(OutputStream outputStream) {
    throw new UnsupportedOperationException("Zstd compression not implemented");
  }
}
