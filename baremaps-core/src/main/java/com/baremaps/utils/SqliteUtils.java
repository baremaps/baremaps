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

package com.baremaps.utils;



import com.google.common.io.Resources;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteConfig.LockingMode;
import org.sqlite.SQLiteConfig.SynchronousMode;
import org.sqlite.SQLiteConfig.TempStore;
import org.sqlite.SQLiteDataSource;

/** A helper class for creating executing sql scripts onto a SQLite database */
public final class SqliteUtils {

  private SqliteUtils() {}

  /**
   * Create a SQLite data source.
   *
   * @param path the path to the SQLite database
   * @return the SQLite data source
   */
  public static DataSource createDataSource(Path path, boolean readOnly) {
    var sqliteConfig = new SQLiteConfig();
    sqliteConfig.setReadOnly(readOnly);
    sqliteConfig.setCacheSize(-1000000);
    sqliteConfig.setPageSize(65536);
    sqliteConfig.setJournalMode(JournalMode.OFF);
    sqliteConfig.setLockingMode(LockingMode.EXCLUSIVE);
    sqliteConfig.setSynchronous(SynchronousMode.OFF);
    sqliteConfig.setTempStore(TempStore.MEMORY);

    var sqliteDataSource = new SQLiteDataSource();
    sqliteDataSource.setConfig(sqliteConfig);
    sqliteDataSource.setUrl("jdbc:sqlite:" + path.toAbsolutePath());

    var hikariConfig = new HikariConfig();
    hikariConfig.setDataSource(sqliteDataSource);
    hikariConfig.setMaximumPoolSize(readOnly ? Runtime.getRuntime().availableProcessors() : 1);

    // The pool has to agree with the connections it hands out: a SQLite connection refuses to
    // change its read-only flag once established, and the pool sets that flag on every connection.
    hikariConfig.setReadOnly(readOnly);

    return new HikariDataSource(hikariConfig);
  }

  /**
   * Executes the queries contained in a resource file.
   *
   * @param databaseUrl the JDBC url
   * @param resource the path of the resource file
   * @throws IOException if the resource cannot be read
   * @throws SQLException if the queries cannot be executed
   */
  public static void executeResource(String databaseUrl, String resource)
      throws IOException, SQLException {
    try (Connection connection = DriverManager.getConnection(databaseUrl)) {
      URL resourceURL = Resources.getResource(resource);
      String queries = Resources.toString(resourceURL, StandardCharsets.UTF_8);
      try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate(queries);
      }
    }
  }
}
