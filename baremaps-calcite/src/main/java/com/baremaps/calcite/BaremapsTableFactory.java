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

import com.baremaps.calcite.csv.CsvTable;
import com.baremaps.calcite.data.DataModifiableTable;
import com.baremaps.calcite.flatgeobuf.FlatGeoBufTable;
import com.baremaps.calcite.geopackage.GeoPackageTable;
import com.baremaps.calcite.geoparquet.GeoParquetTable;
import com.baremaps.calcite.openstreetmap.OpenStreetMapTable;
import com.baremaps.calcite.rpsl.RpslTable;
import com.baremaps.calcite.shapefile.ShapefileTable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.TableFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Creates a table from a {@code format} operand naming the file format and a {@code file} operand
 * pointing at the data. This is the entry point for Calcite model files and for
 * {@code CREATE TABLE ... WITH (...)}.
 */
public class BaremapsTableFactory implements TableFactory<Table> {

  // Calcite does not hand a type factory to table factories, so file formats that must know their
  // row type up front use a default one.
  private static final RelDataTypeFactory TYPE_FACTORY = new JavaTypeFactoryImpl();

  @Override
  public Table create(SchemaPlus schema, String name, Map<String, Object> operand,
      @Nullable RelDataType rowType) {
    String format = require(operand, "format");
    File file = new File(require(operand, "file"));
    try {
      return switch (format) {
        case "data" -> dataTable(file.toPath(), name, rowType);
        case "csv" -> new CsvTable(file,
            ((String) operand.getOrDefault("separator", ",")).charAt(0),
            (Boolean) operand.getOrDefault("hasHeader", true));
        case "shp" -> new ShapefileTable(file);
        case "rpsl" -> new RpslTable(file);
        case "fgb" -> new FlatGeoBufTable(file);
        case "parquet" -> new GeoParquetTable(file, TYPE_FACTORY);
        case "geopackage" -> new GeoPackageTable(file, require(operand, "table"), TYPE_FACTORY);
        case "osm" -> new OpenStreetMapTable(file);
        default -> throw new IllegalArgumentException("Unsupported format: " + format);
      };
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to open " + format + " table " + file, e);
    }
  }

  private static String require(Map<String, Object> operand, String key) {
    Object value = operand.get(key);
    if (value == null) {
      throw new IllegalArgumentException("The '" + key + "' operand must be specified");
    }
    return value.toString();
  }

  /** Opens the table stored in {@code directory}, creating it when the directory is new. */
  private static Table dataTable(Path directory, String name, @Nullable RelDataType rowType) {
    if (Files.isDirectory(directory) && directory.resolve("header").toFile().exists()) {
      return DataModifiableTable.open(directory, TYPE_FACTORY);
    }
    if (rowType == null) {
      throw new IllegalArgumentException("A row type is required to create " + directory);
    }
    return DataModifiableTable.create(directory, name, rowType);
  }
}
