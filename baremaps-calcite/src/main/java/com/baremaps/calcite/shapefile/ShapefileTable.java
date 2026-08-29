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

package com.baremaps.calcite.shapefile;

import com.baremaps.shapefile.Shapefile;
import com.baremaps.shapefile.ShapefileReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
 * A read-only table over a shapefile: its dBASE attributes followed by a {@code geometry} column.
 */
public class ShapefileTable extends AbstractTable implements ScannableTable {

  private final File file;
  private final List<Shapefile.Column> columns;

  public ShapefileTable(File file) throws IOException {
    this.file = file;
    try (ShapefileReader reader = new ShapefileReader(file.toPath())) {
      this.columns = reader.columns();
    }
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    RelDataTypeFactory.Builder builder = typeFactory.builder();
    for (Shapefile.Column column : columns) {
      builder.add(column.name(), typeFactory.createSqlType(sqlType(column)));
    }
    builder.add("geometry", typeFactory.createSqlType(SqlTypeName.GEOMETRY));
    return builder.build();
  }

  /**
   * The SQL type of a column, derived from the Java type the reader produces for it rather than
   * from the type the table declares, so that the two cannot come to disagree.
   */
  private static SqlTypeName sqlType(Shapefile.Column column) {
    Class<?> type = column.javaType();
    if (type == String.class) {
      return SqlTypeName.VARCHAR;
    } else if (type == Long.class) {
      return SqlTypeName.BIGINT;
    } else if (type == Integer.class) {
      return SqlTypeName.INTEGER;
    } else if (type == Double.class) {
      return SqlTypeName.DOUBLE;
    } else if (type == Boolean.class) {
      return SqlTypeName.BOOLEAN;
    } else if (type == LocalDate.class) {
      return SqlTypeName.DATE;
    } else if (type == LocalDateTime.class) {
      return SqlTypeName.TIMESTAMP;
    } else {
      throw new IllegalStateException("Unsupported column type: " + type);
    }
  }

  @Override
  public Enumerable<Object[]> scan(DataContext root) {
    return new AbstractEnumerable<>() {
      @Override
      public Enumerator<Object[]> enumerator() {
        return new ShapefileEnumerator(file);
      }
    };
  }

  private static class ShapefileEnumerator implements Enumerator<Object[]> {

    private final File file;
    private ShapefileReader reader;
    private List<Object> current;

    ShapefileEnumerator(File file) {
      this.file = file;
      open();
    }

    private void open() {
      try {
        reader = new ShapefileReader(file.toPath());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public Object[] current() {
      return current == null ? null : current.toArray();
    }

    @Override
    public boolean moveNext() {
      try {
        current = reader.readRow();
        return current != null;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public void reset() {
      close();
      open();
    }

    @Override
    public void close() {
      reader.close();
    }
  }
}
