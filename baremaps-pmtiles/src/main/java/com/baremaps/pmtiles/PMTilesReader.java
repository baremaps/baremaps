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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Reads tiles and metadata from a PMTiles archive.
 */
public class PMTilesReader implements AutoCloseable {

  /**
   * Leaf directories may point to further leaf directories. Archives in practice nest one level
   * deep; the bound only stops a corrupt archive from looping forever.
   */
  private static final int MAX_DIRECTORY_DEPTH = 4;

  private final FileChannel channel;
  private final Header header;
  private final Directory rootDirectory;

  /**
   * Opens a PMTiles archive.
   *
   * @param path the path of the archive
   * @throws IOException if the archive cannot be opened or does not start with a valid header
   */
  public PMTilesReader(Path path) throws IOException {
    this.channel = FileChannel.open(path);
    try {
      this.header = Header.readFrom(new ByteArrayInputStream(read(0, Header.LENGTH)));
      this.rootDirectory =
          readDirectory(header.rootDirectoryOffset(), header.rootDirectoryLength());
    } catch (IOException e) {
      channel.close();
      throw e;
    }
  }

  /**
   * Returns the header of the archive.
   *
   * @return the header
   */
  public Header getHeader() {
    return header;
  }

  /**
   * Returns a tile of the archive, or {@code null} when the archive does not hold it.
   * <p>
   * The bytes are returned exactly as they are stored, so they are still compressed with
   * {@link Header#tileCompression()}. Servers forward them unchanged, and decompressing here would
   * only force them to compress again.
   *
   * @param z the zoom level
   * @param x the column
   * @param y the row
   * @return the stored tile, or {@code null}
   * @throws IOException if an I/O error occurs
   */
  public ByteBuffer getTile(int z, long x, long y) throws IOException {
    var tileId = TileIdConverter.zxyToTileId(z, x, y);
    var directory = rootDirectory;
    for (var depth = 0; depth < MAX_DIRECTORY_DEPTH; depth++) {
      var entry = directory.find(tileId);
      if (entry == null) {
        return null;
      }
      if (entry.runLength() > 0) {
        return ByteBuffer
            .wrap(read(header.tileDataOffset() + entry.offset(), toLength(entry.length())));
      }
      directory = readDirectory(header.leafDirectoryOffset() + entry.offset(), entry.length());
    }
    throw new IOException("Leaf directories nested more than " + MAX_DIRECTORY_DEPTH + " deep");
  }

  /** Returns the root directory, which every lookup starts from. */
  Directory getRootDirectory() {
    return rootDirectory;
  }

  private Directory readDirectory(long offset, long length) throws IOException {
    return Directory.fromBytes(read(offset, toLength(length)), header.internalCompression());
  }

  /** Reads exactly {@code length} bytes at {@code offset}, which a single channel read may not. */
  private byte[] read(long offset, int length) throws IOException {
    var buffer = ByteBuffer.allocate(length);
    while (buffer.hasRemaining()) {
      if (channel.read(buffer, offset + buffer.position()) < 0) {
        throw new EOFException("Truncated PMTiles archive at offset " + offset);
      }
    }
    return buffer.array();
  }

  private static int toLength(long length) throws IOException {
    if (length < 0 || length > Integer.MAX_VALUE) {
      throw new IOException("Invalid length in PMTiles archive: " + length);
    }
    return (int) length;
  }

  @Override
  public void close() throws IOException {
    channel.close();
  }
}
