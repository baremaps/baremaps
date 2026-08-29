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
import java.net.URI;
import java.util.Map;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

/** A schema exposing a GeoParquet file as a single table named {@value #TABLE_NAME}. */
public class GeoParquetSchema extends AbstractSchema {

  public static final String TABLE_NAME = "geoparquet_table";

  private final Map<String, Table> tableMap;

  public GeoParquetSchema(File file) throws IOException {
    this.tableMap = Map.of(TABLE_NAME, new GeoParquetTable(file, new JavaTypeFactoryImpl()));
  }

  public GeoParquetSchema(URI uri) throws IOException {
    this(new File(uri));
  }

  @Override
  protected Map<String, Table> getTableMap() {
    return tableMap;
  }
}
