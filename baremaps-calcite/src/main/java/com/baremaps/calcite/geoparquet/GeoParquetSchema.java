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
import java.util.HashMap;
import java.util.Map;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

/**
 * A Calcite schema implementation for GeoParquet data. This schema provides access to the single
 * table in a GeoParquet file through the Apache Calcite framework for SQL querying.
 */
public class GeoParquetSchema extends AbstractSchema {

  private final File file;
  private final Map<String, Table> tableMap;
  private static final String DEFAULT_TABLE_NAME = "geoparquet_table";

  /**
   * Constructs a GeoParquetSchema with the specified file.
   *
   * @param file the GeoParquet file to read data from
   * @throws IOException if an I/O error occurs
   */
  public GeoParquetSchema(File file) throws IOException {
    this.file = file;
    this.tableMap = new HashMap<>();

    // Create a table for the GeoParquet file
    tableMap.put(DEFAULT_TABLE_NAME, new GeoParquetTable(file));
  }

  /**
   * Constructs a GeoParquetSchema with the specified URI.
   *
   * @param uri the URI of the GeoParquet file
   * @throws IOException if an I/O error occurs
   */
  public GeoParquetSchema(URI uri) throws IOException {
    this(new File(uri));
  }

  @Override
  protected Map<String, Table> getTableMap() {
    return tableMap;
  }
}
