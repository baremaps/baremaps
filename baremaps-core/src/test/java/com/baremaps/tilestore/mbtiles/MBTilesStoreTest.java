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

package com.baremaps.tilestore.mbtiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.baremaps.data.util.FileUtils;
import com.baremaps.tilestore.TileDataTableSchemaTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;

class MBTilesStoreTest extends TileDataTableSchemaTest {

  Path file;

  @BeforeEach
  void before() throws IOException {
    file = Files.createTempFile(Paths.get("."), "baremaps_", ".tmp");
  }

  @AfterEach
  void after() throws IOException {
    FileUtils.deleteRecursively(file);
  }

  @Override
  public MBTilesStore createTileStore() throws Exception {
    SQLiteDataSource dataSource = new SQLiteDataSource();
    String url = "jdbc:sqlite:" + file.toAbsolutePath();
    dataSource.setUrl(url);
    MBTilesStore tilesStore = new MBTilesStore(dataSource);
    tilesStore.initializeDatabase();
    return tilesStore;
  }

  @Test
  void readWriteMetadata() throws Exception {
    MBTilesStore tileStore = createTileStore();
    Map<String, String> metadata = tileStore.readMetadata();
    assertEquals(0, metadata.size());

    Map<String, String> m1 = new HashMap<>();
    m1.put("test", "test");
    tileStore.writeMetadata(m1);

    Map<String, String> m2 = tileStore.readMetadata();
    assertEquals(1, m2.size());
    assertEquals("test", m2.get("test"));
  }
}
