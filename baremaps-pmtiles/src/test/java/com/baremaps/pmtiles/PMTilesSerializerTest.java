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
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.math.LongMath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PMTilesSerializerTest {

  private final VarIntSerializer varIntSerializer = new VarIntSerializer();
  private final HeaderSerializer headerSerializer = new HeaderSerializer();
  private final EntrySerializer entrySerializer = new EntrySerializer();
  private final DirectorySerializer directorySerializer = new DirectorySerializer();

  @Test
  void decodeVarInt() throws IOException {
    var b = new LittleEndianDataInputStream(new ByteArrayInputStream(new byte[] {
        (byte) 0, (byte) 1,
        (byte) 127, (byte) 0xe5,
        (byte) 0x8e, (byte) 0x26
    }));
    assertEquals(0, varIntSerializer.readVarInt(b));
    assertEquals(1, varIntSerializer.readVarInt(b));
    assertEquals(127, varIntSerializer.readVarInt(b));
    assertEquals(624485, varIntSerializer.readVarInt(b));
    b = new LittleEndianDataInputStream(new ByteArrayInputStream(new byte[] {
        (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0x0f,
    }));
    assertEquals(9007199254740991L, varIntSerializer.readVarInt(b));
  }

  @Test
  void encodeVarInt() throws IOException {
    for (long i = 0; i < 1000; i++) {
      var array = new ByteArrayOutputStream();
      varIntSerializer.writeVarInt(array, i);
      var input = new LittleEndianDataInputStream(new ByteArrayInputStream(array.toByteArray()));
      assertEquals(i, varIntSerializer.readVarInt(input));
    }
    for (long i = Long.MAX_VALUE - 1000; i < Long.MAX_VALUE; i++) {
      var array = new ByteArrayOutputStream();
      varIntSerializer.writeVarInt(array, i);
      var input = new LittleEndianDataInputStream(new ByteArrayInputStream(array.toByteArray()));
      assertEquals(i, varIntSerializer.readVarInt(input));
    }
  }

  @Test
  void zxyToTileId() {
    assertEquals(0, TileIdConverter.zxyToTileId(0, 0, 0));
    assertEquals(1, TileIdConverter.zxyToTileId(1, 0, 0));
    assertEquals(2, TileIdConverter.zxyToTileId(1, 0, 1));
    assertEquals(3, TileIdConverter.zxyToTileId(1, 1, 1));
    assertEquals(4, TileIdConverter.zxyToTileId(1, 1, 0));
    assertEquals(5, TileIdConverter.zxyToTileId(2, 0, 0));
  }

  @Test
  void tileIdToZxy() {
    assertArrayEquals(new long[] {0, 0, 0}, TileIdConverter.tileIdToZxy(0));
    assertArrayEquals(new long[] {1, 0, 0}, TileIdConverter.tileIdToZxy(1));
    assertArrayEquals(new long[] {1, 0, 1}, TileIdConverter.tileIdToZxy(2));
    assertArrayEquals(new long[] {1, 1, 1}, TileIdConverter.tileIdToZxy(3));
    assertArrayEquals(new long[] {1, 1, 0}, TileIdConverter.tileIdToZxy(4));
    assertArrayEquals(new long[] {2, 0, 0}, TileIdConverter.tileIdToZxy(5));
  }

  @Test
  void aLotOfTiles() {
    for (int z = 0; z < 9; z++) {
      for (long x = 0; x < 1 << z; x++) {
        for (long y = 0; y < 1 << z; y++) {
          var result = TileIdConverter.tileIdToZxy(TileIdConverter.zxyToTileId(z, x, y));
          if (result[0] != z || result[1] != x || result[2] != y) {
            fail("roundtrip failed");
          }
        }
      }
    }
  }

  @Test
  void tileExtremes() {
    for (var z = 0; z < 27; z++) {
      var dim = LongMath.pow(2, z) - 1;
      var tl = TileIdConverter.tileIdToZxy(TileIdConverter.zxyToTileId(z, 0, 0));
      assertArrayEquals(new long[] {z, 0, 0}, tl);
      var tr = TileIdConverter.tileIdToZxy(TileIdConverter.zxyToTileId(z, dim, 0));
      assertArrayEquals(new long[] {z, dim, 0}, tr);
      var bl = TileIdConverter.tileIdToZxy(TileIdConverter.zxyToTileId(z, 0, dim));
      assertArrayEquals(new long[] {z, 0, dim}, bl);
      var br = TileIdConverter.tileIdToZxy(TileIdConverter.zxyToTileId(z, dim, dim));
      assertArrayEquals(new long[] {z, dim, dim}, br);
    }
  }

  @Test
  void invalidTiles() {
    assertThrows(RuntimeException.class, () -> TileIdConverter.tileIdToZxy(9007199254740991L));
    assertThrows(RuntimeException.class, () -> TileIdConverter.zxyToTileId(27, 0, 0));
    assertThrows(RuntimeException.class, () -> TileIdConverter.zxyToTileId(0, 1, 1));
  }

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

  @Test
  void searchForMissingEntry() {
    var entries = new ArrayList<Entry>();
    assertNull(entrySerializer.findTile(entries, 101));
  }

  @Test
  void searchForFirstEntry() {
    var entry = Entry.builder().tileId(100).offset(1).length(1).runLength(1).build();
    var entries = new ArrayList<Entry>();
    entries.add(entry);
    assertEquals(entry, entrySerializer.findTile(entries, 100));
  }

  @Test
  void searchWithRunLength() {
    var entry = Entry.builder().tileId(3).offset(3).length(1).runLength(2).build();
    var entries = new ArrayList<Entry>();
    entries.add(entry);
    entries.add(Entry.builder().tileId(5).offset(5).length(1).runLength(2).build());
    assertEquals(entry, entrySerializer.findTile(entries, 4));
  }

  @Test
  void searchWithMultipleTileEntries() {
    var entries = new ArrayList<Entry>();
    entries.add(Entry.builder().tileId(100).offset(1).length(1).runLength(2).build());
    var entry = entrySerializer.findTile(entries, 101);
    assertEquals(1, entry.getOffset());
    assertEquals(1, entry.getLength());

    entries = new ArrayList<Entry>();
    entries.add(Entry.builder().tileId(100).offset(1).length(1).runLength(1).build());
    entries.add(Entry.builder().tileId(150).offset(2).length(2).runLength(2).build());
    entry = entrySerializer.findTile(entries, 151);
    assertEquals(2, entry.getOffset());
    assertEquals(2, entry.getLength());

    entries = new ArrayList<>();
    entries.add(Entry.builder().tileId(50).offset(1).length(1).runLength(2).build());
    entries.add(Entry.builder().tileId(100).offset(2).length(2).runLength(1).build());
    entries.add(Entry.builder().tileId(150).offset(3).length(3).runLength(1).build());
    entry = entrySerializer.findTile(entries, 51);
    assertEquals(1, entry.getOffset());
    assertEquals(1, entry.getLength());
  }

  @Test
  void leafSearch() {
    var entries = new ArrayList<Entry>();
    entries.add(Entry.builder().tileId(100).offset(1).length(1).runLength(0).build());
    var entry = entrySerializer.findTile(entries, 150);
    assertEquals(1, entry.getOffset());
    assertEquals(1, entry.getLength());
  }

  @Test
  void buildRootLeaves() throws IOException {
    var entries = List.of(Entry.builder().tileId(100).offset(1).length(1).runLength(0).build());
    var directories = directorySerializer.buildRootLeaves(entries, 1, Compression.NONE);
    assertEquals(1, directories.getNumLeaves());
  }

  @Test
  void optimizeDirectories() throws IOException {
    var random = new Random(3857);
    var entries = new ArrayList<Entry>();
    entries.add(Entry.builder().tileId(0).offset(0).length(100).runLength(1).build());
    var directories = directorySerializer.optimizeDirectories(entries, 100, Compression.NONE);
    assertFalse(directories.getLeaves().length > 0);
    assertEquals(0, directories.getNumLeaves());

    entries = new ArrayList<>();
    int offset = 0;
    for (var i = 0; i < 1000; i++) {
      var randTileSize = random.nextInt(1000000);
      entries
          .add(Entry.builder().tileId(i).offset(offset).length(randTileSize).runLength(1).build());
      offset += randTileSize;
    }
    directories = directorySerializer.optimizeDirectories(entries, 1024, Compression.NONE);
    assertFalse(directories.getRoot().length > 1024);
    assertNotEquals(0, directories.getNumLeaves());
    assertNotEquals(0, directories.getLeaves().length);
  }
}
