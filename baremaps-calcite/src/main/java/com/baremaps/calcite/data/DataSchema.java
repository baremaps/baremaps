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

import com.baremaps.data.collection.AppendOnlyLog;
import com.baremaps.data.collection.DataCollection;
import com.baremaps.data.memory.Memory;
import com.baremaps.data.memory.MemoryMappedDirectory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

/**
 * A Calcite schema implementation for data stored in directories. This schema provides access to
 * data through the Apache Calcite framework for SQL querying.
 */
public class DataSchema extends AbstractSchema {

  private final File directory;
  private final Map<String, Table> tableMap;
  private final RelDataTypeFactory typeFactory;

  /**
   * Constructs a DataSchema with the specified directory.
   *
   * @param directory the directory containing data subdirectories
   * @param typeFactory the type factory to use for creating tables
   * @throws IOException if an I/O error occurs
   */
  public DataSchema(File directory, RelDataTypeFactory typeFactory) throws IOException {
    this.directory = Objects.requireNonNull(directory, "Directory cannot be null");
    this.typeFactory = Objects.requireNonNull(typeFactory, "Type factory cannot be null");
    this.tableMap = new HashMap<>();

    // Only process directories in the specified directory
    File[] subdirectories = directory.listFiles(File::isDirectory);
    if (subdirectories != null) {
      for (File subdirectory : subdirectories) {
        String tableName = subdirectory.getName();
        Path schemaPath = subdirectory.toPath().resolve("schema.json");

        if (Files.exists(schemaPath)) {
          // Read the schema from the schema.json file
          try (FileInputStream fis = new FileInputStream(schemaPath.toFile())) {
            DataTableSchema schema = DataTableSchema.read(fis, typeFactory);

            // Create the data collection
            DataRowType dataRowType = new DataRowType(schema);
            Memory<MappedByteBuffer> memory = new MemoryMappedDirectory(schemaPath.getParent());
            DataCollection<DataRow> rows = AppendOnlyLog.<DataRow>builder()
                .dataType(dataRowType)
                .memory(memory)
                .build();

            // Create the table
            tableMap.put(tableName, new DataModifiableTable(tableName, schema, rows, typeFactory));
          }
        }
      }
    }
  }

  @Override
  protected Map<String, Table> getTableMap() {
    return tableMap;
  }
}
