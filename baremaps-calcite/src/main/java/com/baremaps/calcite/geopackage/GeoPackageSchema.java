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
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

/** A schema exposing the feature tables of a GeoPackage file. */
public class GeoPackageSchema extends AbstractSchema {

  private final Map<String, Table> tableMap = new HashMap<>();

  public GeoPackageSchema(File file) throws IOException {
    RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
    List<String> featureTables;
    try (GeoPackage geoPackage = GeoPackageManager.open(file)) {
      featureTables = geoPackage.getFeatureTables();
    }
    for (String tableName : featureTables) {
      tableMap.put(tableName, new GeoPackageTable(file, tableName, typeFactory));
    }
  }

  @Override
  protected Map<String, Table> getTableMap() {
    return tableMap;
  }
}
