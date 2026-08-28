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

package com.baremaps.calcite.flatgeobuf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

/**
 * A Calcite schema implementation for FlatGeoBuf data. This schema provides access to FlatGeoBuf
 * files through the Apache Calcite framework for SQL querying.
 */
public class FlatGeoBufSchema extends AbstractSchema {

  private final File directory;
  private final Map<String, Table> tableMap;

  /**
   * Constructs a FlatGeoBufSchema with the specified directory.
   *
   * @param directory the directory containing FlatGeoBuf files
   * @throws IOException if an I/O error occurs
   */
  public FlatGeoBufSchema(File directory) throws IOException {
    this.directory = directory;
    this.tableMap = new HashMap<>();

    // Only process .fgb files in the directory
    File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".fgb"));
    if (files != null) {
      for (File file : files) {
        String tableName = file.getName().replaceFirst("[.][^.]+$", ""); // Remove extension
        tableMap.put(tableName, new FlatGeoBufTable(file));
      }
    }
  }

  @Override
  protected Map<String, Table> getTableMap() {
    return tableMap;
  }
}
