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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class for creating data sources and executing queries.
 */
public final class PostgresUtils {

  private static final Logger logger = LoggerFactory.getLogger(PostgresUtils.class);

  private PostgresUtils() {}

  /**
   * Creates a data source from an object, either a JDBC url or a json representation of a database.
   *
   * @param database the database object, either a JDBC url or a json representation of a database
   * @return the data source
   */
  public static DataSource createDataSourceFromObject(Object database) {
    if (database instanceof String url) {
      return createDataSource(url);
    } else {
      var json = new ObjectMapper().convertValue(database, Database.class);
      return createDataSource(json);
    }
  }

  /**
   * Creates a data source from parameters.
   *
   * @param host the host
   * @param port the port
   * @param database the database
   * @param username the username
   * @param password the password
   * @return the data source
   */
  public static DataSource createDataSource(
      String host,
      Integer port,
      String database,
      String username,
      String password) {
    return createDataSource(
        String.format("jdbc:postgresql://%s:%s/%s?&user=%s&password=%s", host, port,
            database, username, password));
  }

  /**
   * Creates a data source from a JDBC url.
   *
   * @param database the JDBC url
   * @return the data source
   */
  public static DataSource createDataSource(String database) {
    return createDataSource(database, Runtime.getRuntime().availableProcessors() * 2);
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
    config.addDataSourceProperty("allowMultiQueries", true);
    config.addDataSourceProperty("prepareThreshold", 100);

    return new HikariDataSource(config);
  }

  /**
   * Creates a data source from a json representation of a database.
   *
   * @param datasource the object representation of a database
   * @return the data source
   */
  public static DataSource createDataSource(Database datasource) {
    var config = new HikariConfig();
    if (datasource.getDataSourceClassName() != null) {
      config.setDataSourceClassName(datasource.getDataSourceClassName());
    }
    if (datasource.getJdbcUrl() != null) {
      config.setJdbcUrl(datasource.getJdbcUrl());
    }
    if (datasource.getUsername() != null) {
      config.setUsername(datasource.getUsername());
    }
    if (datasource.getPassword() != null) {
      config.setPassword(datasource.getPassword());
    }
    if (datasource.getAutoCommit() != null) {
      config.setAutoCommit(datasource.getAutoCommit());
    }
    if (datasource.getConnectionTimeout() != null) {
      config.setConnectionTimeout(datasource.getConnectionTimeout());
    }
    if (datasource.getIdleTimeout() != null) {
      config.setInitializationFailTimeout(datasource.getIdleTimeout());
    }
    if (datasource.getKeepAliveTime() != null) {
      config.setKeepaliveTime(datasource.getKeepAliveTime());
    }
    if (datasource.getMaxLifetime() != null) {
      config.setMaxLifetime(datasource.getMaxLifetime());
    }
    if (datasource.getMinimumIdle() != null) {
      config.setMinimumIdle(datasource.getMinimumIdle());
    }
    if (datasource.getMaximumPoolSize() != null) {
      config.setMaximumPoolSize(datasource.getMaximumPoolSize());
    }
    if (datasource.getPoolName() != null) {
      config.setPoolName(datasource.getPoolName());
    }
    if (datasource.getReadOnly() != null) {
      config.setReadOnly(datasource.getReadOnly());
    }

    config.addDataSourceProperty("allowMultiQueries", true);
    config.addDataSourceProperty("prepareThreshold", 100);

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
    URL resourceURL = Resources.getResource(resource);
    String queries = Resources.toString(resourceURL, StandardCharsets.UTF_8);
    try (Statement statement = connection.createStatement()) {
      statement.execute(queries);
    }
  }

  /**
   * Gets the version of the Postgres database.
   *
   * @param datasource the data source
   * @return the version of the Postgres database
   * @throws SQLException if a database access error occurs
   */
  public static int getPostgresVersion(DataSource datasource) throws SQLException {
    try (Connection connection = datasource.getConnection()) {
      return connection.getMetaData().getDatabaseMajorVersion();
    }
  }
}
