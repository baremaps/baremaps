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

import com.google.common.io.LittleEndianDataInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads tile data and metadata from a PMTiles file.
 */
public class PMTilesReader implements AutoCloseable {

  private final Path path;
  private final HeaderSerializer headerSerializer;
  private final EntrySerializer entrySerializer;

  private Header header;
  private List<Entry> rootEntries;

  /**
   * Creates a new PMTilesReader for the specified file.
   *
   * @param path the path to the PMTiles file
   */
  public PMTilesReader(Path path) {
    this.path = path;
    this.headerSerializer = new HeaderSerializer();
    this.entrySerializer = new EntrySerializer();
  }

  /**
   * Gets the header of the PMTiles file.
   *
   * @return the header of the PMTiles file
   * @throws IOException if an I/O error occurs
   */
  public Header getHeader() throws IOException {
    if (header == null) {
      try (var inputStream = Files.newInputStream(path)) {
        header = headerSerializer.deserialize(inputStream);
      }
    }
    return header;
  }

  /**
   * Gets the root directory of the PMTiles file.
   *
   * @return the root directory entries
   * @throws IOException if an I/O error occurs
   */
  public List<Entry> getRootDirectory() throws IOException {
    if (rootEntries == null) {
      var header = getHeader();
      rootEntries = getDirectory(header.getRootDirectoryOffset());
    }
    return rootEntries;
  }

  /**
   * Gets a directory from the PMTiles file at the specified offset.
   *
   * @param offset the offset of the directory in the file
   * @return the directory entries
   * @throws IOException if an I/O error occurs
   */
  public List<Entry> getDirectory(long offset) throws IOException {
    var header = getHeader();
    try (var input = Files.newInputStream(path)) {
      long skipped = 0;
      while (skipped < offset) {
        skipped += input.skip(offset - skipped);
      }
      try (var decompressed =
          new LittleEndianDataInputStream(header.getInternalCompression().decompress(input))) {
        return entrySerializer.deserialize(decompressed);
      }
    }
  }

  /**
   * Gets a tile by its coordinates.
   *
   * @param z the zoom level
   * @param x the x coordinate
   * @param y the y coordinate
   * @return the tile data as a ByteBuffer, or null if not found
   * @throws IOException if an I/O error occurs
   */
  public ByteBuffer getTile(int z, long x, long y) throws IOException {
    var tileId = TileIdConverter.zxyToTileId(z, x, y);
    var fileHeader = getHeader();
    var entries = getRootDirectory();
    var entry = entrySerializer.findTile(entries, tileId);

    if (entry == null) {
      return null;
    }

    try (var channel = FileChannel.open(path)) {
      var compressed = ByteBuffer.allocate((int) entry.getLength());
      channel.position(fileHeader.getTileDataOffset() + entry.getOffset());
      channel.read(compressed);
      compressed.flip();
      try (var tile = new ByteArrayInputStream(compressed.array())) {
        return ByteBuffer.wrap(tile.readAllBytes());
      }
    }
  }

  /**
   * Closes the PMTilesReader. No resources need to be closed as all I/O operations use
   * try-with-resources.
   */
  @Override
  public void close() {
    // No resources to close at the class level since all I/O operations use try-with-resources
  }
}
