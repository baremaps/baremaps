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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

/**
 * A schema whose tables are the files of a directory (or a single file) with given extensions. The
 * table name is the file name up to its first dot, so {@code sample.osm.pbf} becomes
 * {@code sample}.
 */
public abstract class FileSchema extends AbstractSchema {

  /** Opens the table stored in a file. */
  @FunctionalInterface
  protected interface TableOpener {
    Table open(File file) throws IOException;
  }

  private final Map<String, Table> tableMap = new HashMap<>();

  /**
   * @param fileOrDirectory a data file, or a directory whose matching files become tables
   * @param extensions the accepted file extensions, including the dot, e.g. {@code ".csv"}
   */
  protected FileSchema(File fileOrDirectory, List<String> extensions, TableOpener opener)
      throws IOException {
    File[] files;
    if (fileOrDirectory.isDirectory()) {
      files = fileOrDirectory.listFiles((dir, name) -> matches(name, extensions));
    } else if (fileOrDirectory.isFile()) {
      files = new File[] {fileOrDirectory};
    } else {
      files = new File[0]; // a missing path is an empty schema, like an empty directory
    }
    if (files == null) {
      return; // the directory cannot be read
    }
    for (File file : files) {
      tableMap.put(tableName(file), opener.open(file));
    }
  }

  private static boolean matches(String fileName, List<String> extensions) {
    String lower = fileName.toLowerCase(Locale.ROOT);
    return extensions.stream().anyMatch(lower::endsWith);
  }

  private static String tableName(File file) {
    String name = file.getName();
    int dot = name.indexOf('.', 1);
    return dot < 0 ? name : name.substring(0, dot);
  }

  @Override
  public Map<String, Table> getTableMap() {
    return tableMap;
  }
}
