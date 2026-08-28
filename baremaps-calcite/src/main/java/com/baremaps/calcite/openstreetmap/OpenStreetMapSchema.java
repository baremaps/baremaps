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

package com.baremaps.calcite.openstreetmap;

import com.baremaps.openstreetmap.EntityReader;
import com.baremaps.openstreetmap.GeometryOptions;
import com.baremaps.openstreetmap.model.Entity;
import com.baremaps.openstreetmap.pbf.PbfEntityReader;
import com.baremaps.openstreetmap.xml.XmlEntityReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

/**
 * A Calcite schema implementation for OpenStreetMap data. This schema provides access to
 * OpenStreetMap files through the Apache Calcite framework for SQL querying.
 */
public class OpenStreetMapSchema extends AbstractSchema {

  private final File directory;
  private final Map<String, Table> tableMap;
  private final RelDataTypeFactory typeFactory;

  /**
   * Constructs an OpenStreetMapSchema with the specified directory.
   *
   * @param directory the directory containing OpenStreetMap files
   * @param typeFactory the type factory to use for creating tables
   * @throws IOException if an I/O error occurs
   */
  public OpenStreetMapSchema(File directory, RelDataTypeFactory typeFactory) throws IOException {
    this.directory = Objects.requireNonNull(directory, "Directory cannot be null");
    this.typeFactory = Objects.requireNonNull(typeFactory, "Type factory cannot be null");
    this.tableMap = new HashMap<>();

    // Process files in the directory
    File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".pbf") ||
        name.toLowerCase().endsWith(".osm.pbf") ||
        name.toLowerCase().endsWith(".xml") ||
        name.toLowerCase().endsWith(".osm"));

    if (files != null) {
      for (File file : files) {
        // Extract the base name without extension (e.g., "sample" from "sample.osm.pbf")
        String fileName = file.getName();
        String tableName = fileName;

        // Remove all extensions (e.g., "sample.osm.pbf" -> "sample")
        while (tableName.contains(".")) {
          int lastDotIndex = tableName.lastIndexOf('.');
          if (lastDotIndex > 0) {
            tableName = tableName.substring(0, lastDotIndex);
          } else {
            break;
          }
        }

        // Create the table with the file reference
        tableMap.put(tableName, createTable(file));
      }
    }
  }

  /**
   * Constructs an OpenStreetMapSchema with a single file.
   *
   * @param file the OpenStreetMap file
   * @param typeFactory the type factory to use for creating tables
   * @throws IOException if an I/O error occurs
   */
  public OpenStreetMapSchema(File file, RelDataTypeFactory typeFactory, boolean isDirectory)
      throws IOException {
    if (isDirectory) {
      // If isDirectory is true, treat the file as a directory
      this.directory = Objects.requireNonNull(file, "Directory cannot be null");
      this.typeFactory = Objects.requireNonNull(typeFactory, "Type factory cannot be null");
      this.tableMap = new HashMap<>();

      // Process files in the directory
      File[] files = file.listFiles((dir, name) -> name.toLowerCase().endsWith(".pbf") ||
          name.toLowerCase().endsWith(".osm.pbf") ||
          name.toLowerCase().endsWith(".xml") ||
          name.toLowerCase().endsWith(".osm"));

      if (files != null) {
        for (File osmFile : files) {
          // Extract the base name without extension (e.g., "sample" from "sample.osm.pbf")
          String fileName = osmFile.getName();
          String tableName = fileName;

          // Remove all extensions (e.g., "sample.osm.pbf" -> "sample")
          while (tableName.contains(".")) {
            int lastDotIndex = tableName.lastIndexOf('.');
            if (lastDotIndex > 0) {
              tableName = tableName.substring(0, lastDotIndex);
            } else {
              break;
            }
          }

          // Create the table with the file reference
          tableMap.put(tableName, createTable(osmFile));
        }
      }
    } else {
      // If isDirectory is false, treat the file as a single file
      this.directory = Objects.requireNonNull(file, "File cannot be null");
      this.typeFactory = Objects.requireNonNull(typeFactory, "Type factory cannot be null");
      this.tableMap = new HashMap<>();

      // Extract the base name without extension (e.g., "sample" from "sample.osm.pbf")
      String fileName = file.getName();
      String tableName = fileName;

      // Remove all extensions (e.g., "sample.osm.pbf" -> "sample")
      while (tableName.contains(".")) {
        int lastDotIndex = tableName.lastIndexOf('.');
        if (lastDotIndex > 0) {
          tableName = tableName.substring(0, lastDotIndex);
        } else {
          break;
        }
      }

      // Create the table with the file reference
      tableMap.put(tableName, createTable(file));
    }
  }

  /**
   * Creates a table for the given file.
   *
   * @param file the OpenStreetMap file
   * @return the created table
   */
  private Table createTable(File file) {
    // Determine the appropriate entity reader based on file extension
    EntityReader<Entity> entityReader;
    if (file.getName().toLowerCase().endsWith(".pbf")) {
      entityReader = new PbfEntityReader(GeometryOptions.inMemory());
    } else {
      entityReader = new XmlEntityReader(GeometryOptions.inMemory());
    }

    // Create the table with the file reference
    return new OpenStreetMapTable(file, entityReader);
  }

  @Override
  protected Map<String, Table> getTableMap() {
    return tableMap;
  }
}
