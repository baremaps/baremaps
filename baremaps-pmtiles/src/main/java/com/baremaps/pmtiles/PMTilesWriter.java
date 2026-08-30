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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Writes tiles and metadata to a PMTiles archive.
 * <p>
 * Tiles are buffered in a temporary file next to the archive as they arrive, because the header
 * that opens the archive can only be filled in once the size of every other section is known. Call
 * {@link #write()} to assemble the archive; closing the writer without doing so discards the tiles.
 */
public class PMTilesWriter implements AutoCloseable {

  /**
   * A reader that fetches the first 16 KB of an archive gets the header and, with any luck, the
   * whole root directory. Sizing the root to what is left of that budget is what makes an archive
   * usable over the network.
   */
  private static final int MAX_ROOT_DIRECTORY_LENGTH = 16384 - Header.LENGTH;

  private final Path path;
  private final Compression compression;
  private final Header.Builder header;
  private final Path tilePath;
  private final OutputStream tileOutput;

  private final List<Entry> entries = new ArrayList<>();
  private final Map<Long, Long> offsetByTileHash = new HashMap<>();
  private Map<String, Object> metadata = Map.of();
  private long tileDataLength;
  private long addressedTiles;
  private Long lastTileHash;
  private boolean clustered = true;
  private boolean closed;

  /**
   * Creates a writer producing a gzipped archive of vector tiles.
   *
   * @param path the path of the archive to write
   * @throws IOException if an I/O error occurs
   */
  public PMTilesWriter(Path path) throws IOException {
    this(path, Compression.GZIP, TileType.MVT);
  }

  /**
   * Creates a writer.
   *
   * @param path the path of the archive to write
   * @param compression the compression to apply to the tiles, the directories and the metadata
   * @param tileType the format of the tiles that will be added
   * @throws IOException if an I/O error occurs
   */
  public PMTilesWriter(Path path, Compression compression, TileType tileType) throws IOException {
    this.path = path;
    this.compression = compression;
    this.header = Header.builder()
        .internalCompression(compression)
        .tileCompression(compression)
        .tileType(tileType);
    // The temporary file shares a directory with the archive so that assembling the archive stays
    // a copy within one filesystem.
    this.tilePath = Files.createTempFile(path.toAbsolutePath().getParent(), "tiles_", ".tmp");
    try {
      this.tileOutput = new BufferedOutputStream(Files.newOutputStream(tilePath));
    } catch (IOException e) {
      Files.deleteIfExists(tilePath);
      throw e;
    }
  }

  /**
   * Sets the metadata describing the tileset, written as JSON.
   *
   * @param metadata the metadata
   */
  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  /**
   * Sets the range of zoom levels the archive covers.
   *
   * @param minZoom the lowest zoom level
   * @param maxZoom the highest zoom level
   */
  public void setZoomRange(int minZoom, int maxZoom) {
    header.zoomRange(minZoom, maxZoom);
  }

  /**
   * Sets the geographic bounds of the archive, in degrees.
   *
   * @param minLon the western edge
   * @param minLat the southern edge
   * @param maxLon the eastern edge
   * @param maxLat the northern edge
   */
  public void setBounds(double minLon, double minLat, double maxLon, double maxLat) {
    header.bounds(minLon, minLat, maxLon, maxLat);
  }

  /**
   * Sets the view a viewer should open the archive at.
   *
   * @param zoom the zoom level
   * @param lon the longitude, in degrees
   * @param lat the latitude, in degrees
   */
  public void setCenter(int zoom, double lon, double lat) {
    header.center(zoom, lon, lat);
  }

  /**
   * Adds a tile to the archive.
   * <p>
   * Tiles that repeat earlier content are stored once and referenced again, so an archive of a
   * sparsely populated area costs little more than the tiles that differ.
   *
   * @param z the zoom level
   * @param x the column
   * @param y the row
   * @param bytes the tile, compressed as declared by the writer
   * @throws IOException if an I/O error occurs
   */
  public void setTile(int z, int x, int y, byte[] bytes) throws IOException {
    checkOpen();
    var tileId = TileIdConverter.zxyToTileId(z, x, y);
    var last = entries.isEmpty() ? null : entries.get(entries.size() - 1);
    addressedTiles++;

    // Once a tile arrives out of order the tile data is no longer laid out by tile id, and runs
    // can no longer be extended; the entries are sorted again before the directories are built.
    if (last != null && tileId < last.tileId()) {
      clustered = false;
    }

    var tileHash = Hashing.farmHashFingerprint64().hashBytes(bytes).asLong();

    // A repeat of the immediately preceding tile extends its run instead of adding an entry, but
    // only while the run stays contiguous: a gap in the tile ids would claim tiles never added.
    if (clustered && last != null && Objects.equals(lastTileHash, tileHash)
        && tileId == last.tileId() + last.runLength()) {
      entries.set(entries.size() - 1, last.withRunLength(last.runLength() + 1));
      return;
    }
    lastTileHash = tileHash;

    var offset = offsetByTileHash.get(tileHash);
    if (offset == null) {
      offset = tileDataLength;
      offsetByTileHash.put(tileHash, offset);
      tileOutput.write(bytes);
      tileDataLength += bytes.length;
    }
    entries.add(new Entry(tileId, offset, bytes.length, 1));
  }

  /**
   * Assembles the archive and closes the writer.
   *
   * @throws IOException if an I/O error occurs
   */
  public void write() throws IOException {
    checkOpen();
    tileOutput.close();

    if (!clustered) {
      entries.sort(Comparator.comparingLong(Entry::tileId));
    }
    var directories = Directories.of(entries, MAX_ROOT_DIRECTORY_LENGTH, compression);
    var metadataBytes = metadataBytes();

    // The sections follow the header back to back, so each offset is the end of the one before.
    var metadataOffset = Header.LENGTH + directories.root().length;
    var leavesOffset = metadataOffset + metadataBytes.length;
    var tilesOffset = leavesOffset + directories.leaves().length;

    var completeHeader = header
        .rootDirectoryOffset(Header.LENGTH)
        .rootDirectoryLength(directories.root().length)
        .jsonMetadataOffset(metadataOffset)
        .jsonMetadataLength(metadataBytes.length)
        .leafDirectoryOffset(leavesOffset)
        .leafDirectoryLength(directories.leaves().length)
        .tileDataOffset(tilesOffset)
        .tileDataLength(tileDataLength)
        .numAddressedTiles(addressedTiles)
        .numTileEntries(entries.size())
        .numTileContents(offsetByTileHash.size())
        .clustered(clustered)
        .build();

    try (var output = new BufferedOutputStream(Files.newOutputStream(path))) {
      completeHeader.writeTo(output);
      output.write(directories.root());
      output.write(metadataBytes);
      output.write(directories.leaves());
      Files.copy(tilePath, output);
    } finally {
      close();
    }
  }

  private byte[] metadataBytes() throws IOException {
    var output = new ByteArrayOutputStream();
    try (var compressed = compression.compress(output)) {
      new ObjectMapper().writeValue(compressed, metadata);
    }
    return output.toByteArray();
  }

  private void checkOpen() throws IOException {
    if (closed) {
      throw new IOException("The PMTiles writer has been closed");
    }
  }

  @Override
  public void close() throws IOException {
    if (closed) {
      return;
    }
    closed = true;
    try {
      tileOutput.close();
    } finally {
      Files.deleteIfExists(tilePath);
    }
  }
}
