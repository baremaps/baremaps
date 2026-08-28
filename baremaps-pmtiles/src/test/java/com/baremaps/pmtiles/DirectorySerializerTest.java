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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Tests for the DirectorySerializer class.
 */
class DirectorySerializerTest {

  private final DirectorySerializer directorySerializer = new DirectorySerializer();

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
