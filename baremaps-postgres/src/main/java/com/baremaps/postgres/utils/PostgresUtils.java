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

package com.baremaps.postgres.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

/**
 * A helper class for creating data sources and executing queries.
 */
public final class PostgresUtils {

  /**
   * How many times a statement is executed before pgjdbc promotes it to a server-side prepared
   * statement. The default of 5 promotes the ad hoc queries of a tile server too eagerly, filling
   * the server's statement cache with plans it will not reuse.
   */
  private static final String PREPARE_THRESHOLD = "100";

  private PostgresUtils() {}

  /**
   * Creates a data source from a configuration, either a JDBC url or the object form of a
   * {@link HikariConfig}.
   *
   * @param database the JDBC url, or a map of Hikari properties such as {@code jdbcUrl} and
   *        {@code maximumPoolSize}
   * @return the data source
   */
  public static DataSource createDataSourceFromObject(Object database) {
    if (database instanceof String url) {
      return createDataSource(url);
    }
    // Hikari already knows how to read its own settings from properties, and its property names are
    // the ones the configuration files use, so there is nothing to translate.
    var fields =
        new ObjectMapper().convertValue(database, new TypeReference<Map<String, Object>>() {});
    var properties = new Properties();
    fields.forEach((name, value) -> {
      if (value != null) {
        properties.setProperty(name, String.valueOf(value));
      }
    });
    return createDataSource(new HikariConfig(properties));
  }

  /**
   * Creates a data source from a JDBC url, sized for the machine it runs on.
   *
   * @param jdbcUrl the JDBC url
   * @return the data source
   */
  public static DataSource createDataSource(String jdbcUrl) {
    // The import and export tasks are IO bound on the database, so they keep more connections busy
    // than the machine has cores.
    return createDataSource(jdbcUrl, Runtime.getRuntime().availableProcessors() * 2);
  }

  /**
   * Creates a data source from a JDBC url with a pool size defined by the user.
   *
   * @param jdbcUrl the JDBC url
   * @param poolSize the pool size
   * @return the data source
   */
  public static DataSource createDataSource(String jdbcUrl, int poolSize) {
    if (poolSize < 1) {
      throw new IllegalArgumentException("PoolSize cannot be inferior to 1");
    }
    var config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);
    config.setMaximumPoolSize(poolSize);
    return createDataSource(config);
  }

  private static DataSource createDataSource(HikariConfig config) {
    config.addDataSourceProperty("prepareThreshold", PREPARE_THRESHOLD);
    return new HikariDataSource(config);
  }

  /**
   * Executes the queries contained in a resource file.
   *
   * @param connection the JDBC connection
   * @param resource the path of the resource file
   * @throws IOException if an I/O error occurs
   * @throws SQLException if a database access error occurs
   */
  public static void executeResource(Connection connection, String resource)
      throws IOException, SQLException {
    var resourceUrl = Resources.getResource(resource);
    var queries = Resources.toString(resourceUrl, StandardCharsets.UTF_8);
    try (Statement statement = connection.createStatement()) {
      statement.execute(queries);
    }
  }

  /**
   * Gets the major version of the Postgres database.
   *
   * @param datasource the data source
   * @return the major version of the Postgres database
   * @throws SQLException if a database access error occurs
   */
  public static int getPostgresVersion(DataSource datasource) throws SQLException {
    try (Connection connection = datasource.getConnection()) {
      return connection.getMetaData().getDatabaseMajorVersion();
    }
  }
}
