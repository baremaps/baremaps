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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A directory of a PMTiles archive: the entries of one root or one leaf, sorted by tile id.
 *
 * @param entries the entries of the directory, in ascending tile id order
 */
record Directory(List<Entry> entries) {

  /**
   * Returns the entry covering a tile id, or {@code null} when the archive does not address it.
   * <p>
   * The result is either a tile entry or a pointer to the leaf directory that continues the search;
   * callers distinguish the two by the run length.
   *
   * @param tileId the tile id to look for
   * @return the entry covering the tile id, or {@code null}
   */
  Entry find(long tileId) {
    var low = 0;
    var high = entries.size() - 1;
    while (low <= high) {
      var middle = (low + high) >>> 1;
      var entry = entries.get(middle);
      if (tileId > entry.tileId()) {
        low = middle + 1;
      } else if (tileId < entry.tileId()) {
        high = middle - 1;
      } else {
        return entry;
      }
    }
    // No entry starts at this tile id, but the one before the insertion point may still cover it:
    // either it is a run of identical tiles, or it is a leaf pointer standing for a whole range.
    if (high >= 0) {
      var entry = entries.get(high);
      if (entry.runLength() == 0 || tileId - entry.tileId() < entry.runLength()) {
        return entry;
      }
    }
    return null;
  }

  /**
   * Serializes and compresses this directory as it is stored in an archive.
   *
   * @param compression the internal compression of the archive
   * @return the bytes of the directory
   * @throws IOException if an I/O error occurs
   */
  byte[] toBytes(Compression compression) throws IOException {
    var output = new ByteArrayOutputStream();
    try (var compressed = compression.compress(output)) {
      writeTo(compressed);
    }
    return output.toByteArray();
  }

  /**
   * Reads a directory from the bytes stored in an archive.
   *
   * @param bytes the bytes of the directory
   * @param compression the internal compression of the archive
   * @return the directory
   * @throws IOException if the bytes do not hold a valid directory
   */
  static Directory fromBytes(byte[] bytes, Compression compression) throws IOException {
    try (var input = compression.decompress(new ByteArrayInputStream(bytes))) {
      return readFrom(input);
    }
  }

  /**
   * Writes the entries as four columns of varints, so that neighbouring values of the same field
   * compress together.
   */
  private void writeTo(OutputStream output) throws IOException {
    VarInt.write(output, entries.size());

    // Tile ids ascend, so only the gap to the previous entry is stored.
    var lastTileId = 0L;
    for (var entry : entries) {
      VarInt.write(output, entry.tileId() - lastTileId);
      lastTileId = entry.tileId();
    }
    for (var entry : entries) {
      VarInt.write(output, entry.runLength());
    }
    for (var entry : entries) {
      VarInt.write(output, entry.length());
    }

    // An offset that continues the previous blob is stored as 0; every other offset is shifted by
    // one so that 0 stays free to act as that marker.
    for (var i = 0; i < entries.size(); i++) {
      var entry = entries.get(i);
      var previous = i == 0 ? null : entries.get(i - 1);
      if (previous != null && entry.offset() == previous.offset() + previous.length()) {
        VarInt.write(output, 0);
      } else {
        VarInt.write(output, entry.offset() + 1);
      }
    }
  }

  private static Directory readFrom(InputStream input) throws IOException {
    var count = VarInt.read(input);
    if (count < 0 || count > Integer.MAX_VALUE) {
      throw new IOException("Invalid directory entry count: " + count);
    }
    var size = (int) count;

    var tileIds = new long[size];
    var lastTileId = 0L;
    for (var i = 0; i < size; i++) {
      lastTileId += VarInt.read(input);
      tileIds[i] = lastTileId;
    }
    var runLengths = new long[size];
    for (var i = 0; i < size; i++) {
      runLengths[i] = VarInt.read(input);
    }
    var lengths = new long[size];
    for (var i = 0; i < size; i++) {
      lengths[i] = VarInt.read(input);
    }
    var offsets = new long[size];
    for (var i = 0; i < size; i++) {
      var value = VarInt.read(input);
      offsets[i] = value == 0 && i > 0 ? offsets[i - 1] + lengths[i - 1] : value - 1;
    }

    var entries = new ArrayList<Entry>(size);
    for (var i = 0; i < size; i++) {
      entries.add(new Entry(tileIds[i], offsets[i], lengths[i], runLengths[i]));
    }
    return new Directory(entries);
  }
}
