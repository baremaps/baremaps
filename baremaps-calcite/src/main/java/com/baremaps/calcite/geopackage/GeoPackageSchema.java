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

package com.baremaps.calcite.geopackage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

/**
 * A Calcite schema implementation for GeoPackage data. This schema provides access to tables in a
 * GeoPackage file through the Apache Calcite framework for SQL querying.
 */
public class GeoPackageSchema extends AbstractSchema {

  private final File file;
  private final Map<String, Table> tableMap;

  /**
   * Constructs a GeoPackageSchema with the specified file.
   *
   * @param file the GeoPackage file to read data from
   * @throws IOException if an I/O error occurs
   */
  public GeoPackageSchema(File file) throws IOException {
    this.file = file;
    this.tableMap = new HashMap<>();

    // Open the GeoPackage file and get all feature tables
    GeoPackage geoPackage = GeoPackageManager.open(file);
    List<String> featureTables = geoPackage.getFeatureTables();

    // Create a table for each feature table
    for (String tableName : featureTables) {
      tableMap.put(tableName, new GeoPackageTable(file, tableName));
    }
  }

  @Override
  protected Map<String, Table> getTableMap() {
    return tableMap;
  }
}
