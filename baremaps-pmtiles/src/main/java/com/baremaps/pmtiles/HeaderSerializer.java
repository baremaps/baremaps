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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Serializer for PMTiles Header objects.
 */
class HeaderSerializer implements Serializer<Header> {

  private static final int HEADER_SIZE_BYTES = 127;
  private static final byte[] MAGIC_BYTES = {0x50, 0x4D, 0x54, 0x69, 0x6C, 0x65, 0x73};

  /**
   * Constructs a new HeaderSerializer.
   */
  HeaderSerializer() {
    // Empty constructor
  }

  @Override
  public void serialize(Header header, OutputStream output) throws IOException {
    var buffer = ByteBuffer.allocate(HEADER_SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN);
    // Write PMTiles magic bytes
    buffer.put(MAGIC_BYTES);

    buffer.put((byte) header.getSpecVersion());
    buffer.putLong(header.getRootDirectoryOffset());
    buffer.putLong(header.getRootDirectoryLength());
    buffer.putLong(header.getJsonMetadataOffset());
    buffer.putLong(header.getJsonMetadataLength());
    buffer.putLong(header.getLeafDirectoryOffset());
    buffer.putLong(header.getLeafDirectoryLength());
    buffer.putLong(header.getTileDataOffset());
    buffer.putLong(header.getTileDataLength());
    buffer.putLong(header.getNumAddressedTiles());
    buffer.putLong(header.getNumTileEntries());
    buffer.putLong(header.getNumTileContents());
    buffer.put((byte) (header.isClustered() ? 1 : 0));
    buffer.put((byte) header.getInternalCompression().ordinal());
    buffer.put((byte) header.getTileCompression().ordinal());
    buffer.put((byte) header.getTileType().ordinal());
    buffer.put((byte) header.getMinZoom());
    buffer.put((byte) header.getMaxZoom());
    buffer.putInt((int) (header.getMinLon() * 10000000));
    buffer.putInt((int) (header.getMinLat() * 10000000));
    buffer.putInt((int) (header.getMaxLon() * 10000000));
    buffer.putInt((int) (header.getMaxLat() * 10000000));
    buffer.put((byte) header.getCenterZoom());
    buffer.putInt((int) (header.getCenterLon() * 10000000));
    buffer.putInt((int) (header.getCenterLat() * 10000000));

    buffer.flip();
    output.write(buffer.array());
  }

  @Override
  public Header deserialize(InputStream input) throws IOException {
    byte[] bytes = new byte[HEADER_SIZE_BYTES];
    var num = input.read(bytes);
    if (num != HEADER_SIZE_BYTES) {
      throw new IOException("Invalid header size");
    }

    var buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

    // Validate magic bytes
    byte[] magic = new byte[7];
    buffer.get(magic);
    for (int i = 0; i < MAGIC_BYTES.length; i++) {
      if (magic[i] != MAGIC_BYTES[i]) {
        throw new IOException("Invalid PMTiles header magic bytes");
      }
    }

    // Use the builder pattern
    return Header.builder()
        .specVersion(buffer.get())
        .rootDirectoryOffset(buffer.getLong())
        .rootDirectoryLength(buffer.getLong())
        .jsonMetadataOffset(buffer.getLong())
        .jsonMetadataLength(buffer.getLong())
        .leafDirectoryOffset(buffer.getLong())
        .leafDirectoryLength(buffer.getLong())
        .tileDataOffset(buffer.getLong())
        .tileDataLength(buffer.getLong())
        .numAddressedTiles(buffer.getLong())
        .numTileEntries(buffer.getLong())
        .numTileContents(buffer.getLong())
        .clustered(buffer.get() == 1)
        .internalCompression(Compression.values()[buffer.get()])
        .tileCompression(Compression.values()[buffer.get()])
        .tileType(TileType.values()[buffer.get()])
        .minZoom(buffer.get())
        .maxZoom(buffer.get())
        .minLon((double) buffer.getInt() / 10000000)
        .minLat((double) buffer.getInt() / 10000000)
        .maxLon((double) buffer.getInt() / 10000000)
        .maxLat((double) buffer.getInt() / 10000000)
        .centerZoom(buffer.get())
        .centerLon((double) buffer.getInt() / 10000000)
        .centerLat((double) buffer.getInt() / 10000000)
        .build();
  }
}
