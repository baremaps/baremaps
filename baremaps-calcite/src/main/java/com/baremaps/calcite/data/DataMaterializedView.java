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


import org.apache.calcite.materialize.MaterializationKey;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.schema.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A table that implements a materialized view. This extends the standard modifiable table with
 * materialization capabilities.
 */
public class DataMaterializedView extends DataModifiableTable {
  /**
   * The key with which this was stored in the materialization service, or null if not (yet)
   * materialized.
   */
  @Nullable
  public MaterializationKey key;

  /**
   * Constructs a new materialized view.
   *
   * @param name the name of the materialized view
   * @param protoRowType the prototype row type
   * @param typeFactory the type factory
   * @throws NullPointerException if name, protoRowType, or typeFactory is null
   */
  public DataMaterializedView(
      String name,
      RelProtoDataType protoRowType,
      RelDataTypeFactory typeFactory) {
    super(name, protoRowType, typeFactory);
    // key is initially null until the materialization is registered
  }

  /**
   * Sets the materialization key.
   *
   * @param key the materialization key
   * @return this materialized view
   */
  public DataMaterializedView withKey(MaterializationKey key) {
    this.key = key;
    return this;
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
