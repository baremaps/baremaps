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
 * Serializer for PMTiles Directory structures.
 */
class DirectorySerializer {

  private final EntrySerializer entrySerializer;

  /**
   * Constructs a new DirectorySerializer.
   */
  DirectorySerializer() {
    this.entrySerializer = new EntrySerializer();
  }

  /**
   * Build root and leaf directories from entries.
   *
   * @param entries the list of entries
   * @param leafSize the number of entries per leaf
   * @param compression the compression to use
   * @return the built directories
   * @throws IOException if an I/O error occurs
   */
  public Directories buildRootLeaves(List<Entry> entries, int leafSize,
      Compression compression) throws IOException {
    var rootEntries = new ArrayList<Entry>();
    var numLeaves = 0;
    byte[] leavesBytes;
    byte[] rootBytes;

    try (var leavesOutput = new ByteArrayOutputStream()) {
      for (var i = 0; i < entries.size(); i += leafSize) {
        numLeaves++;
        var end = i + leafSize;
        if (i + leafSize > entries.size()) {
          end = entries.size();
        }
        var offset = leavesOutput.size();
        try (var leafOutput = new ByteArrayOutputStream()) {
          try (var compressedLeafOutput = compression.compress(leafOutput)) {
            entrySerializer.serialize(entries.subList(i, end), compressedLeafOutput);
          }
          var length = leafOutput.size();
          rootEntries.add(Entry.builder()
              .tileId(entries.get(i).getTileId())
              .offset(offset)
              .length(length)
              .runLength(0)
              .build());
          leavesOutput.write(leafOutput.toByteArray());
        }
      }
      leavesBytes = leavesOutput.toByteArray();
    }

    try (var rootOutput = new ByteArrayOutputStream()) {
      try (var compressedRootOutput = compression.compress(rootOutput)) {
        entrySerializer.serialize(rootEntries, compressedRootOutput);
      }
      rootBytes = rootOutput.toByteArray();
    }

    return Directories.builder()
        .root(rootBytes)
        .leaves(leavesBytes)
        .numLeaves(numLeaves)
        .build();
  }

  /**
   * Optimize directories to fit within targetRootLength.
   *
   * @param entries the list of entries
   * @param targetRootLength the target length of the root directory
   * @param compression the compression to use
   * @return the optimized directories
   * @throws IOException if an I/O error occurs
   */
  public Directories optimizeDirectories(List<Entry> entries, int targetRootLength,
      Compression compression) throws IOException {
    if (entries.size() < 16384) {
      try (var rootOutput = new ByteArrayOutputStream()) {
        try (var compressedOutput = compression.compress(rootOutput)) {
          entrySerializer.serialize(entries, compressedOutput);
        }
        byte[] rootBytes = rootOutput.toByteArray();
        if (rootBytes.length <= targetRootLength) {
          return Directories.builder()
              .root(rootBytes)
              .leaves(new byte[] {})
              .numLeaves(0)
              .build();
        }
      }
    }

    double leafSize = Math.max((double) entries.size() / 3500, 4096);
    while (true) {
      Directories directories = buildRootLeaves(entries, (int) leafSize, compression);
      if (directories.getRoot().length <= targetRootLength) {
        return directories;
      }
      leafSize = leafSize * 1.2;
      // Add a safety check to prevent infinite loops
      if (leafSize > entries.size()) {
        throw new IOException(
            "Could not optimize directories to fit within target length: " + targetRootLength);
      }
    }
  }
}
