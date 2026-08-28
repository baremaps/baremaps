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

import com.baremaps.openstreetmap.GeometryOptions;
import com.baremaps.openstreetmap.pbf.PbfEntityReader;
import com.baremaps.openstreetmap.xml.XmlEntityReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.TableFactory;

/**
 * A table factory for creating OpenStreetMap tables.
 */
public class OpenStreetMapTableFactory implements TableFactory<Table> {

  /**
   * Constructor.
   */
  public OpenStreetMapTableFactory() {}

  @Override
  public Table create(
      SchemaPlus schema,
      String name,
      Map<String, Object> operand,
      RelDataType rowType) {
    // Get the file path from the operand
    String filePath = (String) operand.get("file");
    if (filePath == null) {
      throw new IllegalArgumentException("File path must be specified in the 'file' operand");
    }

    try {
      // Create a new input stream from the file
      InputStream inputStream = new FileInputStream(filePath);

      // Create an entity reader based on the file extension
      if (filePath.endsWith(".pbf") || filePath.endsWith(".osm.pbf")) {
        return createTableFromPbf(Paths.get(filePath));
      } else if (filePath.endsWith(".xml") || filePath.endsWith(".osm")) {
        return createTableFromXml(Paths.get(filePath));
      } else {
        throw new IllegalArgumentException(
            "Unsupported file format. Supported formats are .pbf, .osm.pbf, .xml, and .osm");
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to create OpenStreetMapTable from file: " + filePath, e);
    }
  }

  /**
   * Create a table from a PBF file.
   *
   * @param path the path to the PBF file
   * @return the table
   * @throws IOException if an I/O error occurs
   */
  private OpenStreetMapTable createTableFromPbf(Path path) throws IOException {
    PbfEntityReader reader = new PbfEntityReader(GeometryOptions.inMemory());
    return new OpenStreetMapTable(path.toFile(), reader);
  }

  /**
   * Create a table from an XML file.
   *
   * @param path the path to the XML file
   * @return the table
   * @throws IOException if an I/O error occurs
   */
  private OpenStreetMapTable createTableFromXml(Path path) throws IOException {
    XmlEntityReader reader = new XmlEntityReader(GeometryOptions.inMemory());
    return new OpenStreetMapTable(path.toFile(), reader);
  }
}
