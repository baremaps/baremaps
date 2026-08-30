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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Directory class.
 */
class DirectoryTest {

  @Test
  void findInEmptyDirectory() {
    assertNull(new Directory(List.of()).find(101));
  }

  @Test
  void findExactEntry() {
    var entry = new Entry(100, 1, 1, 1);
    assertEquals(entry, new Directory(List.of(entry)).find(100));
  }

  @Test
  void findWithinRun() {
    var entry = new Entry(3, 3, 1, 2);
    var directory = new Directory(List.of(entry, new Entry(5, 5, 1, 2)));
    assertEquals(entry, directory.find(4));
  }

  @Test
  void findBeyondRun() {
    // The run covers tiles 100 and 101 only, so 102 belongs to no entry.
    var directory = new Directory(List.of(new Entry(100, 1, 1, 2)));
    assertEquals(new Entry(100, 1, 1, 2), directory.find(101));
    assertNull(directory.find(102));
  }

  @Test
  void findAcrossEntries() {
    var directory = new Directory(List.of(
        new Entry(50, 1, 1, 2),
        new Entry(100, 2, 2, 1),
        new Entry(150, 3, 3, 1)));
    assertEquals(new Entry(50, 1, 1, 2), directory.find(51));
    assertEquals(new Entry(100, 2, 2, 1), directory.find(100));
    assertEquals(new Entry(150, 3, 3, 1), directory.find(150));
    assertNull(directory.find(49));
    assertNull(directory.find(101));
  }

  @Test
  void findLeafPointer() {
    // A run length of zero stands for a whole range of tiles continued in a leaf directory.
    var directory = new Directory(List.of(new Entry(100, 1, 1, 0)));
    assertEquals(new Entry(100, 1, 1, 0), directory.find(150));
  }

  @Test
  void writeAndRead() throws IOException {
    var entries = List.of(
        new Entry(0, 0, 10, 1),
        new Entry(1, 10, 20, 1),
        new Entry(2, 30, 5, 3),
        new Entry(100, 500, 7, 1));
    var directory = new Directory(entries);
    for (var compression : List.of(Compression.NONE, Compression.GZIP)) {
      assertEquals(directory, Directory.fromBytes(directory.toBytes(compression), compression));
    }
  }

  @Test
  void writeAndReadEmptyDirectory() throws IOException {
    var directory = new Directory(List.of());
    assertEquals(directory, Directory.fromBytes(directory.toBytes(Compression.NONE),
        Compression.NONE));
  }
}
