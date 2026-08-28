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

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.testing.TestFiles;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import org.junit.jupiter.api.Test;

/**
 * Tests for the HeaderSerializer class.
 */
class HeaderSerializerTest {

  private final HeaderSerializer headerSerializer = new HeaderSerializer();

  @Test
  void decodeHeader() throws IOException {
    var file = TestFiles.resolve("baremaps-testing/data/pmtiles/test_fixture_1.pmtiles");
    try (var channel = FileChannel.open(file)) {
      var input = Channels.newInputStream(channel);
      var header = headerSerializer.deserialize(input);
      assertEquals(127, header.getRootDirectoryOffset());
      assertEquals(25, header.getRootDirectoryLength());
      assertEquals(152, header.getJsonMetadataOffset());
      assertEquals(247, header.getJsonMetadataLength());
      assertEquals(0, header.getLeafDirectoryOffset());
      assertEquals(0, header.getLeafDirectoryLength());
      assertEquals(399, header.getTileDataOffset());
      assertEquals(69, header.getTileDataLength());
      assertEquals(1, header.getNumAddressedTiles());
      assertEquals(1, header.getNumTileEntries());
      assertEquals(1, header.getNumTileContents());
      assertFalse(header.isClustered());
      assertEquals(Compression.GZIP, header.getInternalCompression());
      assertEquals(Compression.GZIP, header.getTileCompression());
      assertEquals(TileType.MVT, header.getTileType());
      assertEquals(0, header.getMinZoom());
      assertEquals(0, header.getMaxZoom());
      assertEquals(0, header.getMinLon());
      assertEquals(0, header.getMinLat());
      assertEquals(1, Math.round(header.getMaxLon()));
      assertEquals(1, Math.round(header.getMaxLat()));
    }
  }

  @Test
  void encodeHeader() throws IOException {
    var header = Header.builder()
        .specVersion(127)
        .rootDirectoryOffset(25)
        .rootDirectoryLength(152)
        .jsonMetadataOffset(247)
        .jsonMetadataLength(0)
        .leafDirectoryOffset(0)
        .leafDirectoryLength(399)
        .tileDataOffset(69)
        .tileDataLength(1)
        .numAddressedTiles(1)
        .numTileEntries(1)
        .numTileContents(10)
        .clustered(false)
        .internalCompression(Compression.GZIP)
        .tileCompression(Compression.GZIP)
        .tileType(TileType.MVT)
        .minZoom(0)
        .maxZoom(0)
        .bounds(0, 1, 1, 0)
        .center(0, 0, 0)
        .build();

    var array = new ByteArrayOutputStream();
    headerSerializer.serialize(header, array);

    var input = new ByteArrayInputStream(array.toByteArray());
    var header2 = headerSerializer.deserialize(input);

    assertEquals(header, header2);
  }
}
