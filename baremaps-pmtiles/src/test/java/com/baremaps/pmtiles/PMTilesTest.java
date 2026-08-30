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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.testing.TestFiles;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Round trips through the PMTiles writer and reader.
 */
class PMTilesTest {

  /** Zoom levels 0 to 2 hold 1 + 4 + 16 tiles, numbered from 0. */
  private static final int LAST_TILE_ID_OF_ZOOM_TWO = 20;

  @TempDir
  Path directory;

  @Test
  void writeAndReadTiles() throws IOException {
    var path = directory.resolve("tiles.pmtiles");
    try (var writer = new PMTilesWriter(path, Compression.NONE, TileType.MVT)) {
      writer.setMetadata(Map.of("name", "test"));
      writer.setZoomRange(0, 2);
      writer.setBounds(-10, -20, 30, 40);
      writer.setCenter(2, 1, 2);
      // Tile ids follow a Hilbert curve, so adding tiles in id order is what makes the archive
      // clustered; a raster scan of the same tiles would not be.
      for (var tileId = 0; tileId <= LAST_TILE_ID_OF_ZOOM_TWO; tileId++) {
        var coord = TileIdConverter.tileIdToZxy(tileId);
        writer.setTile(coord.z(), (int) coord.x(), (int) coord.y(),
            tile(coord.z(), (int) coord.x(), (int) coord.y()));
      }
      writer.write();
    }

    try (var reader = new PMTilesReader(path)) {
      var header = reader.getHeader();
      assertEquals(3, header.specVersion());
      assertEquals(TileType.MVT, header.tileType());
      assertEquals(Compression.NONE, header.tileCompression());
      assertEquals(0, header.minZoom());
      assertEquals(2, header.maxZoom());
      assertEquals(-10, header.minLon(), 1e-6);
      assertEquals(40, header.maxLat(), 1e-6);
      assertEquals(2, header.centerZoom());
      assertTrue(header.clustered());
      assertEquals(21, header.numAddressedTiles());
      assertEquals(21, header.numTileEntries());
      assertEquals(21, header.numTileContents());

      for (var z = 0; z <= 2; z++) {
        for (var x = 0; x < 1 << z; x++) {
          for (var y = 0; y < 1 << z; y++) {
            assertArrayEquals(tile(z, x, y), bytes(reader.getTile(z, x, y)),
                "tile " + z + "/" + x + "/" + y);
          }
        }
      }
      assertNull(reader.getTile(3, 0, 0));
    }
  }

  /**
   * Archives large enough to spill their root directory into leaves must still resolve every tile,
   * which means following the leaf pointers the root holds.
   */
  @Test
  void writeAndReadThroughLeafDirectories() throws IOException {
    var path = directory.resolve("leaves.pmtiles");
    try (var writer = new PMTilesWriter(path, Compression.NONE, TileType.MVT)) {
      writer.setZoomRange(0, 7);
      for (var z = 0; z <= 7; z++) {
        for (var x = 0; x < 1 << z; x++) {
          for (var y = 0; y < 1 << z; y++) {
            writer.setTile(z, x, y, tile(z, x, y));
          }
        }
      }
      writer.write();
    }

    try (var reader = new PMTilesReader(path)) {
      assertTrue(reader.getHeader().leafDirectoryLength() > 0, "expected leaf directories");
      assertTrue(reader.getRootDirectory().entries().size() > 1);
      for (var entry : reader.getRootDirectory().entries()) {
        assertEquals(0, entry.runLength(), "expected every root entry to point to a leaf");
      }

      assertArrayEquals(tile(0, 0, 0), bytes(reader.getTile(0, 0, 0)));
      assertArrayEquals(tile(5, 17, 3), bytes(reader.getTile(5, 17, 3)));
      assertArrayEquals(tile(7, 127, 127), bytes(reader.getTile(7, 127, 127)));
      assertNull(reader.getTile(8, 0, 0));
    }
  }

  /**
   * Repeated tiles collapse into a run, but only while the run stays contiguous: a gap in the tile
   * ids must not silently claim the tiles it skips.
   */
  @Test
  void collapseRepeatedTilesIntoRuns() throws IOException {
    var path = directory.resolve("runs.pmtiles");
    var content = "repeated".getBytes(StandardCharsets.UTF_8);
    var first = TileIdConverter.tileIdToZxy(5);
    var second = TileIdConverter.tileIdToZxy(6);
    var third = TileIdConverter.tileIdToZxy(7);

    try (var writer = new PMTilesWriter(path, Compression.NONE, TileType.MVT)) {
      writer.setTile(first.z(), (int) first.x(), (int) first.y(), content);
      writer.setTile(second.z(), (int) second.x(), (int) second.y(), content);
      writer.setTile(third.z(), (int) third.x(), (int) third.y(), content);
      writer.write();
    }

    try (var reader = new PMTilesReader(path)) {
      var header = reader.getHeader();
      assertEquals(3, header.numAddressedTiles());
      assertEquals(1, header.numTileEntries());
      assertEquals(1, header.numTileContents());
      assertEquals(content.length, header.tileDataLength());

      for (var coord : new TileCoord[] {first, second, third}) {
        assertArrayEquals(content, bytes(reader.getTile(coord.z(), coord.x(), coord.y())));
      }
    }
  }

  @Test
  void doNotExtendRunsAcrossSkippedTiles() throws IOException {
    var path = directory.resolve("gaps.pmtiles");
    var content = "repeated".getBytes(StandardCharsets.UTF_8);
    var first = TileIdConverter.tileIdToZxy(5);
    var skipped = TileIdConverter.tileIdToZxy(6);
    var third = TileIdConverter.tileIdToZxy(7);

    try (var writer = new PMTilesWriter(path, Compression.NONE, TileType.MVT)) {
      writer.setTile(first.z(), (int) first.x(), (int) first.y(), content);
      writer.setTile(third.z(), (int) third.x(), (int) third.y(), content);
      writer.write();
    }

    try (var reader = new PMTilesReader(path)) {
      var header = reader.getHeader();
      assertEquals(2, header.numAddressedTiles());
      assertEquals(2, header.numTileEntries());
      // The repeated content is stored once and pointed at twice.
      assertEquals(1, header.numTileContents());
      assertEquals(content.length, header.tileDataLength());

      assertArrayEquals(content, bytes(reader.getTile(first.z(), first.x(), first.y())));
      assertArrayEquals(content, bytes(reader.getTile(third.z(), third.x(), third.y())));
      assertNull(reader.getTile(skipped.z(), skipped.x(), skipped.y()));
    }
  }

  /**
   * Tiles may arrive in any order, but the archive is only clustered when they arrived sorted, and
   * a reader must not be told otherwise.
   */
  @Test
  void reportUnclusteredArchives() throws IOException {
    var path = directory.resolve("unclustered.pmtiles");
    try (var writer = new PMTilesWriter(path, Compression.NONE, TileType.MVT)) {
      writer.setTile(2, 1, 1, tile(2, 1, 1));
      writer.setTile(1, 0, 0, tile(1, 0, 0));
      writer.setTile(2, 0, 0, tile(2, 0, 0));
      writer.write();
    }

    try (var reader = new PMTilesReader(path)) {
      assertFalse(reader.getHeader().clustered());
      assertArrayEquals(tile(1, 0, 0), bytes(reader.getTile(1, 0, 0)));
      assertArrayEquals(tile(2, 0, 0), bytes(reader.getTile(2, 0, 0)));
      assertArrayEquals(tile(2, 1, 1), bytes(reader.getTile(2, 1, 1)));
    }
  }

  /**
   * Reading an archive written by the reference implementation is the only check that the layout
   * this module writes is the layout the specification describes, rather than merely self
   * consistent.
   */
  @Test
  void readArchiveFromTheReferenceImplementation() throws IOException {
    var file = TestFiles.resolve("baremaps-testing/data/pmtiles/test_fixture_1.pmtiles");
    try (var reader = new PMTilesReader(file)) {
      assertEquals(1, reader.getHeader().numAddressedTiles());

      var tile = bytes(reader.getTile(0, 0, 0));
      assertEquals(69, tile.length);
      // The archive declares gzipped tiles, and the bytes are returned as they are stored.
      assertEquals(Compression.GZIP, reader.getHeader().tileCompression());
      assertEquals((byte) 0x1f, tile[0]);
      assertEquals((byte) 0x8b, tile[1]);

      assertNull(reader.getTile(1, 0, 0));
    }
  }

  @Test
  void writeGzippedArchives() throws IOException {
    var path = directory.resolve("gzipped.pmtiles");
    try (var writer = new PMTilesWriter(path)) {
      writer.setMetadata(Map.of("name", "gzipped"));
      writer.setTile(0, 0, 0, tile(0, 0, 0));
      writer.write();
    }

    try (var reader = new PMTilesReader(path)) {
      assertEquals(Compression.GZIP, reader.getHeader().internalCompression());
      assertArrayEquals(tile(0, 0, 0), bytes(reader.getTile(0, 0, 0)));
    }
  }

  @Test
  void discardTilesWhenTheArchiveIsNeverWritten() throws IOException {
    var path = directory.resolve("abandoned.pmtiles");
    try (var writer = new PMTilesWriter(path, Compression.NONE, TileType.MVT)) {
      writer.setTile(0, 0, 0, tile(0, 0, 0));
    }

    assertFalse(Files.exists(path));
    try (var files = Files.list(directory)) {
      assertTrue(files.noneMatch(file -> file.getFileName().toString().startsWith("tiles_")),
          "expected the temporary tile file to be deleted");
    }
  }

  @Test
  void leaveNoTemporaryFileBehind() throws IOException {
    var path = directory.resolve("temporary.pmtiles");
    try (var writer = new PMTilesWriter(path, Compression.NONE, TileType.MVT)) {
      writer.setTile(0, 0, 0, tile(0, 0, 0));
      writer.write();
    }

    try (var files = Files.list(directory)) {
      assertEquals(1, files.count());
    }
    assertNotNull(path);
  }

  private static byte[] tile(int z, int x, int y) {
    return ("tile-" + z + "-" + x + "-" + y).getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] bytes(ByteBuffer buffer) {
    assertNotNull(buffer, "expected a tile");
    var bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    return bytes;
  }
}
