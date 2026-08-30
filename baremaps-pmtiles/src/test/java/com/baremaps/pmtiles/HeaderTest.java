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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.baremaps.testing.TestFiles;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Header class.
 */
class HeaderTest {

  @Test
  void readFixture() throws IOException {
    var file = TestFiles.resolve("baremaps-testing/data/pmtiles/test_fixture_1.pmtiles");
    try (var input = Files.newInputStream(file)) {
      var header = Header.readFrom(input);
      assertEquals(127, header.rootDirectoryOffset());
      assertEquals(25, header.rootDirectoryLength());
      assertEquals(152, header.jsonMetadataOffset());
      assertEquals(247, header.jsonMetadataLength());
      assertEquals(0, header.leafDirectoryOffset());
      assertEquals(0, header.leafDirectoryLength());
      assertEquals(399, header.tileDataOffset());
      assertEquals(69, header.tileDataLength());
      assertEquals(1, header.numAddressedTiles());
      assertEquals(1, header.numTileEntries());
      assertEquals(1, header.numTileContents());
      assertFalse(header.clustered());
      assertEquals(Compression.GZIP, header.internalCompression());
      assertEquals(Compression.GZIP, header.tileCompression());
      assertEquals(TileType.MVT, header.tileType());
      assertEquals(0, header.minZoom());
      assertEquals(0, header.maxZoom());
      assertEquals(0, header.minLon());
      assertEquals(0, header.minLat());
      assertEquals(1, Math.round(header.maxLon()));
      assertEquals(1, Math.round(header.maxLat()));
    }
  }

  @Test
  void writeAndRead() throws IOException {
    var header = Header.builder()
        .specVersion(3)
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
        .zoomRange(0, 0)
        .bounds(0, 1, 1, 0)
        .center(0, 0, 0)
        .build();

    var output = new ByteArrayOutputStream();
    header.writeTo(output);

    assertEquals(Header.LENGTH, output.size());
    assertEquals(header, Header.readFrom(new ByteArrayInputStream(output.toByteArray())));
  }

  @Test
  void rejectForeignFiles() {
    var bytes = new byte[Header.LENGTH];
    assertThrows(IOException.class, () -> Header.readFrom(new ByteArrayInputStream(bytes)));
  }

  @Test
  void rejectTruncatedFiles() {
    var bytes = new byte[Header.LENGTH - 1];
    assertThrows(IOException.class, () -> Header.readFrom(new ByteArrayInputStream(bytes)));
  }
}
