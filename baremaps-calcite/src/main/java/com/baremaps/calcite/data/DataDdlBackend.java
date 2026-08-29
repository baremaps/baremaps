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

package com.baremaps.calcite.data;

import com.baremaps.calcite.BaremapsTableFactory;
import com.baremaps.calcite.DdlBackend;
import java.util.HashMap;
import java.util.Map;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.materialize.MaterializationService;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.schema.impl.ViewTableMacro;

/**
 * DDL backend whose tables are {@link DataModifiableTable}s.
 *
 * <p>
 * {@code CREATE TABLE ... WITH (format = '...', file = '...')} delegates to
 * {@link BaremapsTableFactory}, so any format it knows can be mounted as a table. Without options
 * the table is held in memory and lost when the connection closes.
 */
public final class DataDdlBackend implements DdlBackend {

  private final BaremapsTableFactory tableFactory = new BaremapsTableFactory();

  @Override
  public void createTable(CalciteSchema schema, String name, RelDataType rowType,
      Map<String, String> options, RelDataTypeFactory typeFactory, Runnable populate) {
    Table table;
    if (options.isEmpty()) {
      table = DataModifiableTable.inMemory(name, rowType);
    } else {
      Map<String, Object> operand = new HashMap<>(options);
      operand.putIfAbsent("format", "data");
      table = tableFactory.create(schema.plus(), name, operand, rowType);
    }
    schema.add(name, table);
    populate.run();
  }

  @Override
  public void dropTable(CalciteSchema schema, String name) {
    schema.removeTable(name);
  }

  @Override
  public void createMaterializedView(CalciteSchema schema, String name, RelDataType rowType,
      String sql, RelDataTypeFactory typeFactory, Runnable populate) {
    DataMaterializedView table = new DataMaterializedView(name, rowType);
    schema.add(name, table);
    populate.run();
    table.key = MaterializationService.instance()
        .defineMaterialization(schema, null, sql, schema.path(null), name, true, true);
  }

  @Override
  public void createView(CalciteSchema schema, String name, String sql, boolean replace,
      ViewTableMacro view) {
    schema.plus().add(name, view);
  }

  @Override
  public void dropView(CalciteSchema schema, String name) {
    schema.removeFunction(name);
  }

  @Override
  public void createSchema(CalciteSchema parent, String name) {
    parent.add(name, new AbstractSchema());
  }

  @Override
  public void dropSchema(CalciteSchema parent, String name) {
    parent.removeSubSchema(name);
  }
}
