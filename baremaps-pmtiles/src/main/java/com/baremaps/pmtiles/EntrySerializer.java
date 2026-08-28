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
import java.util.ArrayList;
import java.util.List;

/**
 * Serializer for PMTiles Entry objects.
 */
class EntrySerializer implements Serializer<List<Entry>> {

  private final VarIntSerializer varIntSerializer;

  /**
   * Constructs a new EntrySerializer.
   */
  EntrySerializer() {
    this.varIntSerializer = new VarIntSerializer();
  }

  /**
   * Serializes a list of entries to an output stream.
   *
   * @param entries the entries to serialize
   * @param output the output stream to write to
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void serialize(List<Entry> entries, OutputStream output) throws IOException {
    var buffer = ByteBuffer.allocate(entries.size() * 48);
    varIntSerializer.writeVarInt(output, entries.size());

    // Write tileIds as deltas
    long lastId = 0;
    for (Entry entry : entries) {
      varIntSerializer.writeVarInt(output, entry.getTileId() - lastId);
      lastId = entry.getTileId();
    }

    // Write run lengths
    for (Entry entry : entries) {
      varIntSerializer.writeVarInt(output, entry.getRunLength());
    }

    // Write lengths
    for (Entry entry : entries) {
      varIntSerializer.writeVarInt(output, entry.getLength());
    }

    // Write offsets (with RLE compression)
    for (int i = 0; i < entries.size(); i++) {
      Entry entry = entries.get(i);
      if (i > 0
          && entry.getOffset() == entries.get(i - 1).getOffset() + entries.get(i - 1).getLength()) {
        varIntSerializer.writeVarInt(output, 0);
      } else {
        varIntSerializer.writeVarInt(output, entry.getOffset() + 1);
      }
    }
  }

  /**
   * Deserializes a list of entries from an input stream.
   *
   * @param input the input stream to read from
   * @return the deserialized list of entries
   * @throws IOException if an I/O error occurs
   */
  @Override
  public List<Entry> deserialize(InputStream input) throws IOException {
    long numEntries = varIntSerializer.readVarInt(input);
    List<Entry> entries = new ArrayList<>((int) numEntries);

    // Read tileIds
    long lastId = 0;
    for (long i = 0; i < numEntries; i++) {
      long value = varIntSerializer.readVarInt(input);
      lastId = lastId + value;
      Entry entry = Entry.builder().tileId(lastId).build();
      entries.add(entry);
    }

    // Read run lengths
    for (long i = 0; i < numEntries; i++) {
      long value = varIntSerializer.readVarInt(input);
      entries.get((int) i).setRunLength(value);
    }

    // Read lengths
    for (long i = 0; i < numEntries; i++) {
      long value = varIntSerializer.readVarInt(input);
      entries.get((int) i).setLength(value);
    }

    // Read offsets
    for (long i = 0; i < numEntries; i++) {
      long value = varIntSerializer.readVarInt(input);
      if (value == 0 && i > 0) {
        Entry prevEntry = entries.get((int) i - 1);
        entries.get((int) i).setOffset(prevEntry.getOffset() + prevEntry.getLength());
      } else {
        entries.get((int) i).setOffset(value - 1);
      }
    }

    return entries;
  }

  /**
   * Find a tile entry by its tile ID.
   *
   * @param entries list of entries to search
   * @param tileId the tile ID to find
   * @return the entry if found, null otherwise
   */
  public Entry findTile(List<Entry> entries, long tileId) {
    int m = 0;
    int n = entries.size() - 1;
    while (m <= n) {
      int k = (n + m) >> 1;
      long cmp = tileId - entries.get(k).getTileId();
      if (cmp > 0) {
        m = k + 1;
      } else if (cmp < 0) {
        n = k - 1;
      } else {
        return entries.get(k);
      }
    }

    // at this point, m > n
    if (n >= 0) {
      if (entries.get(n).getRunLength() == 0) {
        return entries.get(n);
      }
      if (tileId - entries.get(n).getTileId() < entries.get(n).getRunLength()) {
        return entries.get(n);
      }
    }
    return null;
  }
}
