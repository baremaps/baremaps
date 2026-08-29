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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.calcite.geopackage.GeoPackageSchema;
import com.baremaps.tasks.PostgresImport;
import com.baremaps.testing.PostgresContainerTest;
import com.baremaps.testing.TestFiles;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class GeoPackageToPostgresTest extends PostgresContainerTest {

  @Test
  @Tag("integration")
  void copyGeoPackageToPostgres() throws Exception {
    GeoPackageSchema schema = new GeoPackageSchema(TestFiles.GEOPACKAGE.toFile());
    Map<String, String> tables = new LinkedHashMap<>();
    for (String table : schema.getTableNames()) {
      tables.put(table, table);
    }
    assertFalse(tables.isEmpty(), "No tables found in GeoPackage");

    Map<String, Long> counts = PostgresImport.copy(dataSource(), schema, tables, 4326);

    for (Map.Entry<String, Long> count : counts.entrySet()) {
      assertTrue(count.getValue() > 0, "Expected rows in table: " + count.getKey());
      try (Connection connection = dataSource().getConnection();
          Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT EXISTS (SELECT 1 FROM "
              + "information_schema.tables WHERE table_name = '" + count.getKey() + "')")) {
        assertTrue(resultSet.next() && resultSet.getBoolean(1),
            "Failed to create table: " + count.getKey());
      }
    }
  }
}
