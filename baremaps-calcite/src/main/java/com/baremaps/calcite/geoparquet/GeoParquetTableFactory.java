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

package com.baremaps.calcite.geoparquet;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.TableFactory;

/**
 * A table factory for creating GeoParquet tables.
 */
public class GeoParquetTableFactory implements TableFactory<Table> {

  /**
   * Constructor.
   */
  public GeoParquetTableFactory() {}

  @Override
  public Table create(
      SchemaPlus schema,
      String name,
      Map<String, Object> operand,
      RelDataType rowType) {
    if (operand.size() < 2) {
      throw new IllegalArgumentException("Missing file path for GeoParquet table");
    }
    try {
      String filePath = (String) operand.get("file");
      if (filePath == null) {
        throw new IllegalArgumentException("File path must be specified in the 'file' operand");
      }

      // Create a type factory - Calcite doesn't expose one through SchemaPlus
      RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();

      return new GeoParquetTable(new File(filePath), typeFactory);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create GeoParquet table", e);
    }
  }
}
