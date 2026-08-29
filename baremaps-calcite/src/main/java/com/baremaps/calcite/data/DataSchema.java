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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

/** A schema exposing every memory-mapped table found in the sub-directories of a directory. */
public class DataSchema extends AbstractSchema {

  private final Map<String, Table> tableMap = new HashMap<>();

  public DataSchema(File directory, RelDataTypeFactory typeFactory) {
    File[] subdirectories = directory.listFiles(File::isDirectory);
    if (subdirectories != null) {
      for (File subdirectory : subdirectories) {
        DataModifiableTable table = DataModifiableTable.open(subdirectory.toPath(), typeFactory);
        tableMap.put(table.name(), table);
      }
    }
  }

  @Override
  protected Map<String, Table> getTableMap() {
    return tableMap;
  }
}
