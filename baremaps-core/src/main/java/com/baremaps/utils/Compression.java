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

package com.baremaps.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

public enum Compression {
  NONE,
  GZIP,
  BZIP2;

  public static Compression detect(Path file) {
    if (file.toString().endsWith(".gz")) {
      return GZIP;
    } else if (file.toString().endsWith(".bz2")) {
      return BZIP2;
    } else {
      return NONE;
    }
  }

  public InputStream decompress(InputStream inputStream) throws IOException {
    return switch (this) {
      case GZIP -> new GZIPInputStream(inputStream);
      case BZIP2 -> new BZip2CompressorInputStream(inputStream);
      default -> inputStream;
    };
  }

  public OutputStream compress(OutputStream outputStream) throws IOException {
    return switch (this) {
      case GZIP -> new GZIPOutputStream(outputStream);
      case BZIP2 -> new BZip2CompressorOutputStream(outputStream);
      default -> outputStream;
    };
  }
}
