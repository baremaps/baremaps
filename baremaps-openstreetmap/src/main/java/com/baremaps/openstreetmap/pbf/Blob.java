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

package com.baremaps.openstreetmap.pbf;

import com.baremaps.osm.binary.Fileformat;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * A raw, still compressed, block of an OpenStreetMap PBF file.
 *
 * @param type the block type, either "OSMHeader" or "OSMData"
 * @param rawData the encoded {@code Fileformat.Blob}
 */
record Blob(String type, byte[] rawData) {

  /**
   * Decodes and inflates the blob.
   *
   * @return the uncompressed content
   */
  ByteString data() throws DataFormatException, InvalidProtocolBufferException {
    Fileformat.Blob blob = Fileformat.Blob.parseFrom(rawData);
    if (blob.hasRaw()) {
      return blob.getRaw();
    }
    if (!blob.hasZlibData()) {
      throw new DataFormatException("Unsupported blob compression");
    }
    return ByteString.copyFrom(inflate(blob.getZlibData().toByteArray(), blob.getRawSize()));
  }

  /**
   * Inflates the bytes, whose uncompressed size the blob header announces.
   *
   * <p>
   * A truncated blob inflates to fewer bytes than announced without raising anything, and the
   * partially filled buffer would then fail to parse as protobuf, far from the actual cause. The
   * size is therefore checked here.
   */
  private static byte[] inflate(byte[] compressed, int rawSize) throws DataFormatException {
    byte[] bytes = new byte[rawSize];
    Inflater inflater = new Inflater();
    try {
      inflater.setInput(compressed);
      int inflated = inflater.inflate(bytes);
      if (inflated != rawSize) {
        throw new DataFormatException(
            "Truncated blob: inflated %d of the %d announced bytes".formatted(inflated, rawSize));
      }
      return bytes;
    } finally {
      inflater.end();
    }
  }
}
