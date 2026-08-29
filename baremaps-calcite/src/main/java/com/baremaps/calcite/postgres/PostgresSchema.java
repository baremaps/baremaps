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

import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A Calcite schema mirroring a PostgreSQL schema. Tables, views and materialized views are
 * discovered on demand from the catalog, so objects created outside Calcite are visible.
 *
 * <p>
 * Registering this schema is also what enables {@link PostgresDdlExecutor} to create objects in
 * PostgreSQL: it takes the {@code DataSource} from the schema targeted by the statement.
 */
public class PostgresSchema extends AbstractSchema {

  private final DataSource dataSource;
  private final String name;
  private final RelDataTypeFactory typeFactory;

  /**
   * @param dataSource the database
   * @param name the PostgreSQL schema name, e.g. {@code public}
   * @param typeFactory the type factory used to build row types
   */
  public PostgresSchema(DataSource dataSource, String name, RelDataTypeFactory typeFactory) {
    this.dataSource = dataSource;
    this.name = name;
    this.typeFactory = typeFactory;
  }

  public DataSource dataSource() {
    return dataSource;
  }

  public String name() {
    return name;
  }

  @Override
  protected Map<String, Table> getTableMap() {
    return new TableMap();
  }

  /**
   * A live view of the catalog: names are listed and tables opened on demand, so that objects
   * created outside Calcite are visible without loading every table up front.
   */
  private class TableMap extends AbstractMap<String, Table> {

    private List<String> names() {
      try {
        return PostgresCatalog.tableNames(dataSource, name);
      } catch (SQLException e) {
        throw new IllegalStateException("Failed to list the tables of schema " + name, e);
      }
    }

    @Override
    public @Nullable Table get(Object key) {
      if (!(key instanceof String tableName) || !names().contains(tableName)) {
        return null;
      }
      try {
        return new PostgresModifiableTable(dataSource, name, tableName, typeFactory);
      } catch (SQLException e) {
        throw new IllegalStateException("Failed to read table " + tableName, e);
      }
    }

    @Override
    public boolean containsKey(Object key) {
      return key instanceof String tableName && names().contains(tableName);
    }

    @Override
    public Set<String> keySet() {
      return new LinkedHashSet<>(names());
    }

    @Override
    public Set<Entry<String, Table>> entrySet() {
      Set<Entry<String, Table>> entries = new LinkedHashSet<>();
      for (String tableName : names()) {
        entries.add(new SimpleImmutableEntry<>(tableName, get(tableName)));
      }
      return entries;
    }
  }
}
