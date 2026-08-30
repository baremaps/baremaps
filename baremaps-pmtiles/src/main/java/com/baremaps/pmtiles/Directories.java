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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The serialized root and leaf directory sections of an archive.
 *
 * @param root the bytes of the root directory
 * @param leaves the bytes of the concatenated leaf directories, empty when the root holds every
 *        entry
 */
record Directories(byte[] root, byte[] leaves) {

  /**
   * Above this many entries a single directory is assumed not to fit the root budget, and the cost
   * of serializing it just to find out is not worth paying.
   */
  private static final int MAX_SINGLE_DIRECTORY_ENTRIES = 16384;

  /** The number of root entries that fills the root budget once compressed. */
  private static final int TARGET_ROOT_ENTRIES = 3500;

  /** Leaves smaller than this cost more in extra requests than they save in root bytes. */
  private static final int MIN_LEAF_ENTRIES = 4096;

  /**
   * Arranges entries into a root directory that fits within a byte budget, spilling into leaf
   * directories when it does not.
   * <p>
   * A root that fits a single range request is what lets a reader open a remote archive without
   * downloading it, so the budget is the constraint the whole layout is solved for.
   *
   * @param entries the entries of the archive, in ascending tile id order
   * @param targetRootLength the largest acceptable root directory, in bytes
   * @param compression the internal compression of the archive
   * @return the root and leaf sections
   * @throws IOException if no arrangement fits the budget
   */
  static Directories of(List<Entry> entries, int targetRootLength, Compression compression)
      throws IOException {
    if (entries.size() < MAX_SINGLE_DIRECTORY_ENTRIES) {
      var root = new Directory(entries).toBytes(compression);
      if (root.length <= targetRootLength) {
        return new Directories(root, new byte[0]);
      }
    }

    // Start from the leaf size that would leave about TARGET_ROOT_ENTRIES in the root, then grow
    // the leaves until the root fits.
    var leafSize = Math.max(entries.size() / TARGET_ROOT_ENTRIES, MIN_LEAF_ENTRIES);
    while (true) {
      var directories = split(entries, leafSize, compression);
      if (directories.root().length <= targetRootLength) {
        return directories;
      }
      if (leafSize >= entries.size()) {
        throw new IOException(
            "Could not fit the root directory within " + targetRootLength + " bytes");
      }
      leafSize += leafSize / 5;
    }
  }

  /**
   * Splits the entries into leaves of at most {@code leafSize} entries and a root pointing to them.
   */
  private static Directories split(List<Entry> entries, int leafSize, Compression compression)
      throws IOException {
    var rootEntries = new ArrayList<Entry>();
    var leaves = new ByteArrayOutputStream();
    for (var i = 0; i < entries.size(); i += leafSize) {
      var end = Math.min(i + leafSize, entries.size());
      var leaf = new Directory(entries.subList(i, end)).toBytes(compression);
      // A run length of zero marks the entry as a pointer to the leaf rather than to a tile.
      rootEntries.add(new Entry(entries.get(i).tileId(), leaves.size(), leaf.length, 0));
      leaves.writeBytes(leaf);
    }
    return new Directories(new Directory(rootEntries).toBytes(compression), leaves.toByteArray());
  }
}
