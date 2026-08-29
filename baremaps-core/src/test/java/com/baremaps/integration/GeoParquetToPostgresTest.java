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

package com.baremaps.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.calcite.geoparquet.GeoParquetSchema;
import com.baremaps.tasks.PostgresImport;
import com.baremaps.testing.PostgresContainerTest;
import com.baremaps.testing.TestFiles;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class GeoParquetToPostgresTest extends PostgresContainerTest {

  @Test
  @Tag("integration")
  void copyGeoParquetToPostgres() throws Exception {
    Map<String, Long> counts = PostgresImport.copy(dataSource(),
        new GeoParquetSchema(TestFiles.GEOPARQUET.toUri()),
        Map.of(GeoParquetSchema.TABLE_NAME, "geoparquet"), 4326);

    assertEquals(1, counts.size());
    assertTrue(counts.get("geoparquet") > 0, "Expected rows in table geoparquet");
    try (Connection connection = dataSource().getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM geoparquet")) {
      assertTrue(resultSet.next());
      assertEquals(counts.get("geoparquet"), resultSet.getLong(1));
    }
  }
}
