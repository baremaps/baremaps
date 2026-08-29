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
import com.baremaps.openstreetmap.model.Element;
import com.baremaps.openstreetmap.model.Entity;
import com.baremaps.openstreetmap.model.Node;
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.openstreetmap.model.Way;
import com.baremaps.openstreetmap.pbf.PbfEntityReader;
import com.baremaps.openstreetmap.xml.XmlEntityReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Locale;
import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;

/**
 * A read-only table over an OpenStreetMap file ({@code .pbf}, {@code .osm} or {@code .xml}) with
 * one row per node, way and relation. Geometries are built in memory while reading.
 */
public class OpenStreetMapTable extends AbstractTable implements ScannableTable {

  private final File file;
  private final EntityReader<Entity> entityReader;

  public OpenStreetMapTable(File file) {
    this.file = file;
    this.entityReader = file.getName().toLowerCase(Locale.ROOT).endsWith(".pbf")
        ? new PbfEntityReader(GeometryOptions.inMemory())
        : new XmlEntityReader(GeometryOptions.inMemory());
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    RelDataType varchar = typeFactory.createSqlType(SqlTypeName.VARCHAR);
    return typeFactory.builder()
        .add("id", SqlTypeName.BIGINT)
        .add("type", SqlTypeName.VARCHAR)
        .add("version", SqlTypeName.INTEGER)
        .add("timestamp", SqlTypeName.TIMESTAMP)
        .add("uid", SqlTypeName.INTEGER)
        .add("user", SqlTypeName.VARCHAR)
        .add("changeset", SqlTypeName.BIGINT)
        .add("tags", typeFactory.createMapType(varchar, varchar))
        .add("geometry", SqlTypeName.GEOMETRY)
        .build();
  }

  @Override
  public Enumerable<Object[]> scan(DataContext root) {
    return new AbstractEnumerable<>() {
      @Override
      public Enumerator<Object[]> enumerator() {
        return new OpenStreetMapEnumerator();
      }
    };
  }

  private class OpenStreetMapEnumerator implements Enumerator<Object[]> {

    private InputStream inputStream;
    private Iterator<Element> elements;
    private Object[] current;

    OpenStreetMapEnumerator() {
      open();
    }

    private void open() {
      try {
        inputStream = new FileInputStream(file);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      elements = entityReader.read(inputStream)
          .filter(Element.class::isInstance)
          .map(Element.class::cast)
          .iterator();
    }

    @Override
    public Object[] current() {
      return current;
    }

    @Override
    public boolean moveNext() {
      if (!elements.hasNext()) {
        return false;
      }
      Element element = elements.next();
      current = new Object[] {
          element.getId(),
          type(element),
          element.getInfo().version(),
          element.getInfo().timestamp(),
          element.getInfo().uid(),
          "", // the user name is not part of Info
          element.getInfo().changeset(),
          element.getTags(),
          element.getGeometry()
      };
      return true;
    }

    private static String type(Element element) {
      if (element instanceof Node) {
        return "node";
      } else if (element instanceof Way) {
        return "way";
      } else if (element instanceof Relation) {
        return "relation";
      }
      return "unknown";
    }

    @Override
    public void reset() {
      close();
      open();
    }

    @Override
    public void close() {
      try {
        inputStream.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
