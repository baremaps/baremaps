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

package com.baremaps.calcite;

import java.util.Map;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.impl.ViewTableMacro;

/**
 * The storage side of DDL execution.
 *
 * <p>
 * {@link BaremapsDdlExecutor} does everything that is expressible in terms of Calcite alone (name
 * resolution, row-type derivation, {@code IF [NOT] EXISTS}, populating a table from a query). A
 * backend does the rest: it creates or drops the physical object and installs (or removes) its
 * Calcite wrapper in the given schema. Keeping installation on the backend side lets each backend
 * order "create physical object", "install wrapper" and "populate" as its storage requires.
 */
public interface DdlBackend {

  /**
   * Creates a table and installs it in {@code schema}, then runs {@code populate}, which inserts
   * the rows of a {@code CREATE TABLE ... AS query} (a no-op otherwise). The table must be
   * installed before {@code populate} runs because population is an {@code INSERT} that resolves
   * the table by name.
   *
   * @param options the {@code WITH (...)} options of the statement, possibly empty
   */
  void createTable(CalciteSchema schema, String name, RelDataType rowType,
      Map<String, String> options, RelDataTypeFactory typeFactory, Runnable populate);

  /** Drops a table or materialized view and removes it from {@code schema}. */
  void dropTable(CalciteSchema schema, String name);

  /**
   * Creates a materialized view of {@code sql} and installs it in {@code schema}. Backends without
   * native materialized views create a table and run {@code populate} to fill it.
   */
  void createMaterializedView(CalciteSchema schema, String name, RelDataType rowType, String sql,
      RelDataTypeFactory typeFactory, Runnable populate);

  /** Creates a view of {@code sql} and installs {@code view} in {@code schema}. */
  void createView(CalciteSchema schema, String name, String sql, boolean replace,
      ViewTableMacro view);

  /** Drops a view and removes it from {@code schema}. */
  void dropView(CalciteSchema schema, String name);

  /** Creates a sub-schema of {@code parent}. */
  void createSchema(CalciteSchema parent, String name);

  /** Drops a sub-schema of {@code parent}. */
  void dropSchema(CalciteSchema parent, String name);
}
