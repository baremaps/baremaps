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

package com.baremaps.calcite.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.materialize.MaterializationKey;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A table that implements a materialized view for PostgreSQL. This extends the
 * PostgresModifiableTable with materialization capabilities.
 */
public class PostgresMaterializedView extends PostgresModifiableTable {

  /**
   * The key with which this was stored in the materialization service, or null if not (yet)
   * materialized.
   */
  @Nullable
  public MaterializationKey key;

  /**
   * Constructs a new PostgreSQL materialized view.
   *
   * @param dataSource the data source for the PostgreSQL connection
   * @param tableName the name of the materialized view
   * @throws SQLException if an SQL error occurs
   */
  public PostgresMaterializedView(DataSource dataSource, String tableName) throws SQLException {
    super(dataSource, tableName);
    // key is initially null until the materialization is registered
  }

  /**
   * Constructs a new PostgreSQL materialized view with specified type factory.
   *
   * @param dataSource the data source for the PostgreSQL connection
   * @param tableName the name of the materialized view
   * @param typeFactory the type factory
   * @throws SQLException if an SQL error occurs
   */
  public PostgresMaterializedView(DataSource dataSource, String tableName,
      RelDataTypeFactory typeFactory) throws SQLException {
    super(dataSource, tableName, typeFactory);
    // key is initially null until the materialization is registered
  }

  /**
   * Sets the materialization key.
   *
   * @param key the materialization key
   * @return this materialized view
   */
  public PostgresMaterializedView withKey(MaterializationKey key) {
    this.key = key;
    return this;
  }

  /**
   * Refreshes the materialized view with the latest data.
   * 
   * @param concurrent whether to allow concurrent refresh (if supported by PostgreSQL version)
   * @return true if the refresh was successful
   * @throws SQLException if an SQL error occurs
   */
  public boolean refreshMaterializedView(boolean concurrent) throws SQLException {
    try (Connection connection = getDataSource().getConnection()) {
      String refreshSql = "REFRESH MATERIALIZED VIEW ";
      if (concurrent) {
        refreshSql += "CONCURRENTLY ";
      }
      refreshSql += "\"" + getTableName() + "\"";

      try (PreparedStatement stmt = connection.prepareStatement(refreshSql)) {
        return stmt.executeUpdate() >= 0; // Statement success
      }
    }
  }

  @Override
  public Enumerable<Object[]> scan(DataContext root) {
    // For materialized views, we might want to refresh it first
    // But for now, just delegate to the parent implementation
    return super.scan(root);
  }

  @Override
  public Schema.TableType getJdbcTableType() {
    return Schema.TableType.MATERIALIZED_VIEW;
  }

  @Override
  public <C> @Nullable C unwrap(Class<C> aClass) {
    if (MaterializationKey.class.isAssignableFrom(aClass)
        && aClass.isInstance(key)) {
      return aClass.cast(key);
    }
    return super.unwrap(aClass);
  }
}
