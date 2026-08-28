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

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/**
 * Tests for the EntrySerializer class.
 */
class EntrySerializerTest {

  private final EntrySerializer entrySerializer = new EntrySerializer();

  @Test
  void searchForMissingEntry() {
    var entries = new ArrayList<Entry>();
    assertNull(entrySerializer.findTile(entries, 101));
  }

  @Test
  void searchForFirstEntry() {
    var entry = Entry.builder().tileId(100).offset(1).length(1).runLength(1).build();
    var entries = new ArrayList<Entry>();
    entries.add(entry);
    assertEquals(entry, entrySerializer.findTile(entries, 100));
  }

  @Test
  void searchWithRunLength() {
    var entry = Entry.builder().tileId(3).offset(3).length(1).runLength(2).build();
    var entries = new ArrayList<Entry>();
    entries.add(entry);
    entries.add(Entry.builder().tileId(5).offset(5).length(1).runLength(2).build());
    assertEquals(entry, entrySerializer.findTile(entries, 4));
  }

  @Test
  void searchWithMultipleTileEntries() {
    var entries = new ArrayList<Entry>();
    entries.add(Entry.builder().tileId(100).offset(1).length(1).runLength(2).build());
    var entry = entrySerializer.findTile(entries, 101);
    assertEquals(1, entry.getOffset());
    assertEquals(1, entry.getLength());

    entries = new ArrayList<Entry>();
    entries.add(Entry.builder().tileId(100).offset(1).length(1).runLength(1).build());
    entries.add(Entry.builder().tileId(150).offset(2).length(2).runLength(2).build());
    entry = entrySerializer.findTile(entries, 151);
    assertEquals(2, entry.getOffset());
    assertEquals(2, entry.getLength());

    entries = new ArrayList<>();
    entries.add(Entry.builder().tileId(50).offset(1).length(1).runLength(2).build());
    entries.add(Entry.builder().tileId(100).offset(2).length(2).runLength(1).build());
    entries.add(Entry.builder().tileId(150).offset(3).length(3).runLength(1).build());
    entry = entrySerializer.findTile(entries, 51);
    assertEquals(1, entry.getOffset());
    assertEquals(1, entry.getLength());
  }

  @Test
  void leafSearch() {
    var entries = new ArrayList<Entry>();
    entries.add(Entry.builder().tileId(100).offset(1).length(1).runLength(0).build());
    var entry = entrySerializer.findTile(entries, 150);
    assertEquals(1, entry.getOffset());
    assertEquals(1, entry.getLength());
  }
}
