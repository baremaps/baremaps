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

import com.baremaps.postgres.metadata.DatabaseMetadata;
import com.baremaps.postgres.metadata.TableMetadata;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

public class PostgresSchema extends AbstractSchema {

  private final DataSource dataSource;
  private final String schemaName;
  private final RelDataTypeFactory typeFactory;

  public PostgresSchema(DataSource dataSource, String schemaName, RelDataTypeFactory typeFactory) {
    this.dataSource = dataSource;
    this.schemaName = schemaName;
    this.typeFactory = typeFactory;
  }

  @Override
  protected Map<String, Table> getTableMap() {
    DatabaseMetadata databaseMetadata = new DatabaseMetadata(dataSource);
    Map<String, Table> tableMap = new HashMap<>();
    try {
      List<TableMetadata> tables =
          databaseMetadata.getTableMetaData(null, schemaName, null, new String[] {"TABLE"});
      for (TableMetadata table : tables) {
        String tableName = table.table().tableName();
        Table calciteTable = new PostgresModifiableTable(dataSource, tableName, typeFactory);
        tableMap.put(tableName, calciteTable);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return tableMap;
  }
}
