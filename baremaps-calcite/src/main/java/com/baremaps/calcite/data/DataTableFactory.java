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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.TableFactory;

/**
 * A table factory for creating Data tables.
 */
public class DataTableFactory implements TableFactory<Table> {

  private static final RelDataTypeFactory TYPE_FACTORY = new JavaTypeFactoryImpl();

  /**
   * Constructor.
   */
  public DataTableFactory() {}

  @Override
  public Table create(
      SchemaPlus schema,
      String name,
      Map<String, Object> operand,
      RelDataType rowType) {
    String file = (String) operand.get("file");
    if (file == null) {
      throw new RuntimeException("A file should be specified");
    }

    try {
      Memory<MappedByteBuffer> memory = new MemoryMappedDirectory(Paths.get(file));
      ByteBuffer header = memory.header();

      // For new tables, initialize with schema
      if (rowType != null) {
        // Create and serialize schema
        Map<String, Object> schemaMap = new HashMap<>();
        schemaMap.put("name", name);
        schemaMap.put("columns", rowType.getFieldList().stream()
            .map(field -> {
              Map<String, Object> column = new HashMap<>();
              column.put("name", field.getName());
              column.put("cardinality",
                  field.getType().isNullable() ? DataColumn.Cardinality.OPTIONAL.name()
                      : DataColumn.Cardinality.REQUIRED.name());
              column.put("sqlTypeName", field.getType().getSqlTypeName().name());
              return column;
            })
            .toList());

        // Serialize and write schema to header
        byte[] schemaBytes = new ObjectMapper().writeValueAsBytes(schemaMap);
        header.putLong(0L);
        header.putInt(schemaBytes.length);
        header.put(schemaBytes);
      }

      // Read schema and create table
      header.position(0);
      long size = header.getLong();
      int length = header.getInt();
      byte[] bytes = new byte[length];
      header.get(bytes);
      DataTableSchema dataSchema =
          DataTableSchema.read(new ByteArrayInputStream(bytes), TYPE_FACTORY);
      DataRowType dataRowType = new DataRowType(dataSchema);
      DataCollection<DataRow> dataCollection = AppendOnlyLog.<DataRow>builder()
          .dataType(dataRowType)
          .memory(memory)
          .build();

      return new DataModifiableTable(name, dataSchema, dataCollection, TYPE_FACTORY);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
