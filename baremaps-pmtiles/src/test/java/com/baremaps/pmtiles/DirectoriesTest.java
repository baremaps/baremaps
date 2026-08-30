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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Directories class.
 */
class DirectoriesTest {

  @Test
  void keepEverythingInTheRootWhenItFits() throws IOException {
    var entries = List.of(new Entry(0, 0, 100, 1));
    var directories = Directories.of(entries, 100, Compression.NONE);

    assertEquals(0, directories.leaves().length);
    assertEquals(entries, Directory.fromBytes(directories.root(), Compression.NONE).entries());
  }

  @Test
  void spillIntoLeavesWhenTheRootDoesNotFit() throws IOException {
    var random = new Random(3857);
    var entries = new ArrayList<Entry>();
    var offset = 0L;
    for (var i = 0; i < 1000; i++) {
      var length = random.nextInt(1000000);
      entries.add(new Entry(i, offset, length, 1));
      offset += length;
    }

    var directories = Directories.of(entries, 1024, Compression.NONE);

    assertTrue(directories.root().length <= 1024);
    assertTrue(directories.leaves().length > 0);

    // Every root entry points to a leaf, and the leaves hold the original entries in order.
    var root = Directory.fromBytes(directories.root(), Compression.NONE);
    var leafEntries = new ArrayList<Entry>();
    for (var pointer : root.entries()) {
      assertEquals(0, pointer.runLength());
      var bytes = new byte[(int) pointer.length()];
      System.arraycopy(directories.leaves(), (int) pointer.offset(), bytes, 0, bytes.length);
      leafEntries.addAll(Directory.fromBytes(bytes, Compression.NONE).entries());
    }
    assertEquals(entries, leafEntries);
  }
}
