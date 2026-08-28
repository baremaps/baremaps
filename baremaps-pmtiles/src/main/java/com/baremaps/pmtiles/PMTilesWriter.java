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
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Writes tile data and metadata to a PMTiles file. Supports various compression formats and tile
 * types.
 */
public class PMTilesWriter implements AutoCloseable {

  private final Compression compression;
  private final Path path;
  private final List<Entry> entries;
  private final Map<Long, Long> tileHashToOffset;
  private final Path tilePath;
  private final HeaderSerializer headerSerializer;
  private final DirectorySerializer directorySerializer;
  private boolean isClosed = false;

  private Map<String, Object> metadata = new HashMap<>();
  private Long lastTileHash = null;
  private boolean clustered = true;
  private int minZoom = 0;
  private int maxZoom = 14;
  private double minLon = -180;
  private double minLat = -90;
  private double maxLon = 180;
  private double maxLat = 90;
  private int centerZoom = 3;
  private double centerLat = 0;
  private double centerLon = 0;

  /**
   * Creates a new PMTilesWriter with default compression (GZIP).
   *
   * @param path the path where the PMTiles file will be written
   * @throws IOException if an I/O error occurs
   */
  public PMTilesWriter(Path path) throws IOException {
    this(path, new ArrayList<>(), new HashMap<>(), Compression.GZIP);
  }

  /**
   * Creates a new PMTilesWriter with custom parameters.
   *
   * @param path the path where the PMTiles file will be written
   * @param entries the initial list of tile entries
   * @param tileHashToOffset mapping of tile hash to file offset
   * @param compression the compression algorithm to use
   * @throws IOException if an I/O error occurs
   */
  public PMTilesWriter(Path path, List<Entry> entries, Map<Long, Long> tileHashToOffset,
      Compression compression)
      throws IOException {
    this.compression = compression;
    this.path = path;
    this.entries = entries;
    this.tileHashToOffset = tileHashToOffset;

    Path tempPath = null;
    try {
      tempPath = Files.createTempFile(path.getParent(), "tiles_", ".tmp");
      this.tilePath = tempPath;
      this.headerSerializer = new HeaderSerializer();
      this.directorySerializer = new DirectorySerializer();
    } catch (IOException e) {
      if (tempPath != null && Files.exists(tempPath)) {
        try {
          Files.delete(tempPath);
        } catch (IOException ex) {
          e.addSuppressed(ex);
        }
      }
      throw e;
    }
  }

  /**
   * Sets the metadata for the PMTiles file.
   *
   * @param metadata the metadata to include in the file
   */
  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  /**
   * Adds a tile to the PMTiles file.
   *
   * @param z the zoom level
   * @param x the x coordinate
   * @param y the y coordinate
   * @param bytes the tile data
   * @throws IOException if an I/O error occurs
   */
  public void setTile(int z, int x, int y, byte[] bytes) throws IOException {
    if (isClosed) {
      throw new IOException("PMTilesWriter has been closed");
    }

    // Write the tile
    var tileId = TileIdConverter.zxyToTileId(z, x, y);
    var tileLength = bytes.length;
    Long tileHash = Hashing.farmHashFingerprint64().hashBytes(bytes).asLong();

    // If the tile is not greater than the last one, the index is not clustered
    if (!entries.isEmpty() && tileId < entries.get(entries.size() - 1).getTileId()) {
      clustered = false;
    }

    // If the tile is the same as the last one, increment the run length
    if (clustered && tileHash.equals(lastTileHash)) {
      var lastEntry = entries.get(entries.size() - 1);
      lastEntry.setRunLength(lastEntry.getRunLength() + 1);
    }

    // Else, if the tile is the same as the last one, increment the run length
    else if (tileHashToOffset.containsKey(tileHash)) {
      var tileOffset = tileHashToOffset.get(tileHash);
      entries.add(Entry.builder()
          .tileId(tileId)
          .offset(tileOffset)
          .length(tileLength)
          .runLength(1)
          .build());
    }

    // Else, write the tile and add it to the index
    else {
      var tileOffset = Files.size(tilePath);
      tileHashToOffset.put(tileHash, tileOffset);
      lastTileHash = tileHash;
      try (var output = new FileOutputStream(tilePath.toFile(), true)) {
        output.write(bytes);
        entries.add(Entry.builder()
            .tileId(tileId)
            .offset(tileOffset)
            .length(tileLength)
            .runLength(1)
            .build());
      }
    }
  }

  /**
   * Sets the minimum zoom level for the PMTiles file.
   *
   * @param minZoom the minimum zoom level
   */
  public void setMinZoom(int minZoom) {
    this.minZoom = minZoom;
  }

  /**
   * Sets the maximum zoom level for the PMTiles file.
   *
   * @param maxZoom the maximum zoom level
   */
  public void setMaxZoom(int maxZoom) {
    this.maxZoom = maxZoom;
  }

  /**
   * Sets the minimum longitude for the PMTiles file bounds.
   *
   * @param minLon the minimum longitude
   */
  public void setMinLon(double minLon) {
    this.minLon = minLon;
  }

  /**
   * Sets the minimum latitude for the PMTiles file bounds.
   *
   * @param minLat the minimum latitude
   */
  public void setMinLat(double minLat) {
    this.minLat = minLat;
  }

  /**
   * Sets the maximum longitude for the PMTiles file bounds.
   *
   * @param maxLon the maximum longitude
   */
  public void setMaxLon(double maxLon) {
    this.maxLon = maxLon;
  }

  /**
   * Sets the maximum latitude for the PMTiles file bounds.
   *
   * @param maxLat the maximum latitude
   */
  public void setMaxLat(double maxLat) {
    this.maxLat = maxLat;
  }

  /**
   * Sets the center zoom level for the PMTiles file.
   *
   * @param centerZoom the center zoom level
   */
  public void setCenterZoom(int centerZoom) {
    this.centerZoom = centerZoom;
  }

  /**
   * Sets the center latitude for the PMTiles file.
   *
   * @param centerLat the center latitude
   */
  public void setCenterLat(double centerLat) {
    this.centerLat = centerLat;
  }

  /**
   * Sets the center longitude for the PMTiles file.
   *
   * @param centerLon the center longitude
   */
  public void setCenterLon(double centerLon) {
    this.centerLon = centerLon;
  }

  /**
   * Writes all tiles and metadata to the PMTiles file.
   *
   * @throws IOException if an I/O error occurs
   */
  public void write() throws IOException {
    if (isClosed) {
      throw new IOException("PMTilesWriter has been closed");
    }

    // Sort the entries by tile id
    if (!clustered) {
      entries.sort(Comparator.comparingLong(Entry::getTileId));
    }

    var directories = directorySerializer.optimizeDirectories(entries, 16247, compression);

    byte[] metadataBytes;
    try (var metadataOutput = new ByteArrayOutputStream()) {
      try (var compressedMetadataOutput = compression.compress(metadataOutput)) {
        new ObjectMapper().writeValue(compressedMetadataOutput, metadata);
      }
      metadataBytes = metadataOutput.toByteArray();
    }

    var rootOffset = 127;
    var rootLength = directories.getRoot().length;
    var metadataOffset = rootOffset + rootLength;
    var metadataLength = metadataBytes.length;
    var leavesOffset = metadataOffset + metadataLength;
    var leavesLength = directories.getLeaves().length;
    var tilesOffset = leavesOffset + leavesLength;
    var tilesLength = Files.size(tilePath);
    var numTiles = entries.size();

    // Use builder pattern for creating Header
    var header = Header.builder()
        .numAddressedTiles(numTiles)
        .numTileEntries(numTiles)
        .numTileContents(tileHashToOffset.size())
        .clustered(true)
        .internalCompression(compression)
        .tileCompression(compression)
        .tileType(TileType.MVT)
        .rootDirectoryOffset(rootOffset)
        .rootDirectoryLength(rootLength)
        .jsonMetadataOffset(metadataOffset)
        .jsonMetadataLength(metadataLength)
        .leafDirectoryOffset(leavesOffset)
        .leafDirectoryLength(leavesLength)
        .tileDataOffset(tilesOffset)
        .tileDataLength(tilesLength)
        .minZoom(minZoom)
        .maxZoom(maxZoom)
        .bounds(minLon, minLat, maxLon, maxLat)
        .center(centerZoom, centerLon, centerLat)
        .build();

    try (var output = new FileOutputStream(path.toFile())) {
      headerSerializer.serialize(header, output);
      output.write(directories.getRoot());
      output.write(metadataBytes);
      output.write(directories.getLeaves());
      Files.copy(tilePath, output);
    } finally {
      close();
    }
  }

  /**
   * Closes the PMTilesWriter and releases resources.
   *
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void close() throws IOException {
    if (!isClosed) {
      if (Files.exists(tilePath)) {
        Files.delete(tilePath);
      }
      isClosed = true;
    }
  }
}
