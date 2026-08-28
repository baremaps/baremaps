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

package com.baremaps.testing;



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;

public abstract class PostgresRepositoryTest extends PostgresContainerTest {

  @BeforeEach
  public void initializeDatabase() throws SQLException, IOException {
    DataSource dataSource = dataSource();
    try (Connection connection = dataSource.getConnection()) {
      execute(connection, "baremaps-postgres/src/test/resources/queries/osm_create_extensions.sql");
      execute(connection, "baremaps-postgres/src/test/resources/queries/osm_drop_tables.sql");
      execute(connection, "baremaps-postgres/src/test/resources/queries/osm_create_tables.sql");
    }
  }

  /**
   * Executes the queries contained in a resource file.
   *
   * @param connection the JDBC connection
   * @param file the path of the resource file
   * @throws IOException if an I/O error occurs
   * @throws SQLException if a database access error occurs
   */
  public static void execute(Connection connection, String file)
      throws IOException, SQLException {
    Path path = TestFiles.resolve(file);
    String queries = Files.readString(path);
    try (Statement statement = connection.createStatement()) {
      statement.execute(queries);
    }
  }
}
