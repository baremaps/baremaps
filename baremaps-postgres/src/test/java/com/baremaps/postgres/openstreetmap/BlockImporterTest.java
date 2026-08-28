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

package com.baremaps.postgres.openstreetmap;

import static com.baremaps.testing.TestFiles.SAMPLE_OSM_PBF;
import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.openstreetmap.pbf.PbfBlockReader;
import com.baremaps.postgres.utils.PostgresUtils;
import com.baremaps.testing.PostgresContainerTest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class BlockImporterTest extends PostgresContainerTest {

  public DataSource dataSource;
  public HeaderRepository headerRepository;
  public NodeRepository nodeRepository;
  public WayRepository wayRepository;
  public RelationRepository relationRepository;

  @BeforeEach
  void init() throws SQLException, IOException {
    dataSource = PostgresUtils.createDataSource(jdbcUrl(), 1);
    headerRepository = new HeaderRepository(dataSource);
    nodeRepository = new NodeRepository(dataSource);
    wayRepository = new WayRepository(dataSource);
    relationRepository = new RelationRepository(dataSource);
    try (Connection connection = dataSource.getConnection()) {
      PostgresUtils.executeResource(connection, "queries/osm_create_extensions.sql");
      PostgresUtils.executeResource(connection, "queries/osm_drop_tables.sql");
      PostgresUtils.executeResource(connection, "queries/osm_create_tables.sql");
    }
  }

  @Test
  @Tag("integration")
  void test() throws RepositoryException, IOException {
    // Import data
    BlockImporter blockImporter =
        new BlockImporter(headerRepository, nodeRepository, wayRepository, relationRepository);

    try (InputStream inputStream = Files.newInputStream(SAMPLE_OSM_PBF)) {

      new PbfBlockReader().read(inputStream).forEach(blockImporter);

      // Check node importation
      for (long i = 1; i <= 36; i++) {
        var exists = false;
        exists |= nodeRepository.get(i) != null;
        exists |= wayRepository.get(i) != null;
        exists |= relationRepository.get(i) != null;
        assertTrue(exists);
      }
    }
  }
}
