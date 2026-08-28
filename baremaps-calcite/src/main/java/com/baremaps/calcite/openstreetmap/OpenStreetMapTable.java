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
import com.baremaps.openstreetmap.model.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;
import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.locationtech.jts.geom.Geometry;

/**
 * A Calcite table implementation for OpenStreetMap data. This table reads entities from an
 * OpenStreetMap PBF file and makes them available through the Apache Calcite framework for SQL
 * querying.
 */
public class OpenStreetMapTable extends AbstractTable implements ScannableTable {

  private final File file;
  private final EntityReader<Entity> entityReader;
  private RelDataType rowType;

  /**
   * Constructs an OpenStreetMapTable with the specified parameters.
   *
   * @param file the OpenStreetMap file
   * @param entityReader the EntityReader for parsing the OSM data
   */
  public OpenStreetMapTable(File file, EntityReader<Entity> entityReader) {
    this.file = Objects.requireNonNull(file, "File cannot be null");
    this.entityReader = Objects.requireNonNull(entityReader, "Entity reader cannot be null");
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    if (rowType == null) {
      rowType = createRowType(typeFactory);
    }
    return rowType;
  }

  /**
   * Creates the row type (schema) for the OpenStreetMap data.
   *
   * @param typeFactory the type factory
   * @return the RelDataType representing the schema
   */
  private RelDataType createRowType(RelDataTypeFactory typeFactory) {
    RelDataTypeFactory.Builder builder = typeFactory.builder();

    // Define the columns
    builder.add("id", SqlTypeName.BIGINT);
    builder.add("type", SqlTypeName.VARCHAR);
    builder.add("version", SqlTypeName.INTEGER);
    builder.add("timestamp", SqlTypeName.TIMESTAMP);
    builder.add("uid", SqlTypeName.INTEGER);
    builder.add("user", SqlTypeName.VARCHAR);
    builder.add("changeset", SqlTypeName.BIGINT);

    // Create a map type for tags (createMapType only takes two parameters, not three)
    RelDataType keyType = typeFactory.createSqlType(SqlTypeName.VARCHAR);
    RelDataType valueType = typeFactory.createSqlType(SqlTypeName.VARCHAR);
    RelDataType mapType = typeFactory.createMapType(keyType, valueType);
    builder.add("tags", mapType);

    builder.add("geometry", typeFactory.createJavaType(Geometry.class));

    return builder.build();
  }

  @Override
  public Enumerable<Object[]> scan(DataContext root) {
    return new AbstractEnumerable<Object[]>() {
      @Override
      public Enumerator<Object[]> enumerator() {
        try {
          return new OpenStreetMapEnumerator(entityReader, new FileInputStream(file));
        } catch (IOException e) {
          throw new RuntimeException("Failed to open input stream", e);
        }
      }
    };
  }

  /**
   * Enumerator for OpenStreetMap data.
   */
  private static class OpenStreetMapEnumerator implements Enumerator<Object[]> {
    private final EntityReader<Entity> entityReader;
    private final InputStream inputStream;
    private Iterator<Element> iterator;
    private Object[] current;

    public OpenStreetMapEnumerator(EntityReader<Entity> entityReader, InputStream inputStream) {
      this.entityReader = entityReader;
      this.inputStream = inputStream;
      initialize();
    }

    private void initialize() {
      Stream<Element> elementStream = entityReader.read(inputStream)
          .filter(entity -> entity instanceof Element)
          .map(entity -> (Element) entity);
      this.iterator = elementStream.iterator();
    }

    @Override
    public Object[] current() {
      return current;
    }

    @Override
    public boolean moveNext() {
      if (iterator.hasNext()) {
        Element element = iterator.next();
        current = elementToRow(element);
        return true;
      }
      return false;
    }

    @Override
    public void reset() {
      try {
        if (inputStream.markSupported()) {
          inputStream.reset();
          initialize();
        } else {
          throw new UnsupportedOperationException("Reset not supported for this input stream");
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to reset the stream", e);
      }
    }

    @Override
    public void close() {
      try {
        inputStream.close();
      } catch (Exception e) {
        // Ignore
      }
    }

    /**
     * Converts an Element to a row array.
     *
     * @param element the OSM Element
     * @return the corresponding row array
     */
    private Object[] elementToRow(Element element) {
      return new Object[] {
          element.getId(),
          elementTypeToString(element),
          element.getInfo().version(),
          element.getInfo().timestamp(),
          element.getInfo().uid(),
          "", // User name is not available in the Info class, using empty string
          element.getInfo().changeset(),
          element.getTags(),
          element.getGeometry()
      };
    }

    /**
     * Converts the element type to a String.
     *
     * @param element the OSM Element
     * @return the element type as String
     */
    private String elementTypeToString(Element element) {
      if (element instanceof Node) {
        return "node";
      } else if (element instanceof Way) {
        return "way";
      } else if (element instanceof Relation) {
        return "relation";
      } else {
        return "unknown";
      }
    }
  }
}
