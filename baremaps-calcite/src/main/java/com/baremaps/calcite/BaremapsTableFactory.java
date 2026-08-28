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

import com.baremaps.calcite.csv.CsvTableFactory;
import com.baremaps.calcite.data.DataTableFactory;
import com.baremaps.calcite.flatgeobuf.FlatGeoBufTableFactory;
import com.baremaps.calcite.geopackage.GeoPackageTableFactory;
import com.baremaps.calcite.geoparquet.GeoParquetTableFactory;
import com.baremaps.calcite.openstreetmap.OpenStreetMapTableFactory;
import com.baremaps.calcite.rpsl.RpslTableFactory;
import com.baremaps.calcite.shapefile.ShapefileTableFactory;
import java.util.Map;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.TableFactory;

/**
 * A table factory for creating tables in the calcite2 package. This factory routes to the
 * appropriate specialized factory based on the format.
 */
public class BaremapsTableFactory implements TableFactory<Table> {

  private final CsvTableFactory csvTableFactory;
  private final DataTableFactory dataTableFactory;
  private final FlatGeoBufTableFactory flatGeoBufTableFactory;
  private final GeoPackageTableFactory geoPackageTableFactory;
  private final GeoParquetTableFactory geoParquetTableFactory;
  private final OpenStreetMapTableFactory openStreetMapTableFactory;
  private final RpslTableFactory rpslTableFactory;
  private final ShapefileTableFactory shapefileTableFactory;

  /**
   * Constructor.
   */
  public BaremapsTableFactory() {
    this.csvTableFactory = new CsvTableFactory();
    this.dataTableFactory = new DataTableFactory();
    this.flatGeoBufTableFactory = new FlatGeoBufTableFactory();
    this.geoPackageTableFactory = new GeoPackageTableFactory();
    this.geoParquetTableFactory = new GeoParquetTableFactory();
    this.openStreetMapTableFactory = new OpenStreetMapTableFactory();
    this.rpslTableFactory = new RpslTableFactory();
    this.shapefileTableFactory = new ShapefileTableFactory();
  }

  @Override
  public Table create(
      SchemaPlus schema,
      String name,
      Map<String, Object> operand,
      RelDataType rowType) {
    String format = (String) operand.get("format");
    if (format == null) {
      throw new IllegalArgumentException("Format must be specified in the 'format' operand");
    }

    return switch (format) {
      case "data" -> dataTableFactory.create(schema, name, operand, rowType);
      case "osm" -> openStreetMapTableFactory.create(schema, name, operand, rowType);
      case "csv" -> csvTableFactory.create(schema, name, operand, rowType);
      case "shp" -> shapefileTableFactory.create(schema, name, operand, rowType);
      case "rpsl" -> rpslTableFactory.create(schema, name, operand, rowType);
      case "fgb" -> flatGeoBufTableFactory.create(schema, name, operand, rowType);
      case "parquet" -> geoParquetTableFactory.create(schema, name, operand, rowType);
      case "geopackage" -> geoPackageTableFactory.create(schema, name, operand, rowType);
      default -> throw new IllegalArgumentException("Unsupported format: " + format);
    };
  }
}
