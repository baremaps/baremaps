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

import com.baremaps.shapefile.DBaseFieldDescriptor;
import com.baremaps.shapefile.ShapefileInputStream;
import com.baremaps.shapefile.ShapefileReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
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
  private final List<DBaseFieldDescriptor> fieldDescriptors;

  public ShapefileTable(File file) throws IOException {
    this.file = file;
    try (ShapefileReader reader = new ShapefileReader(file.getPath())) {
      this.fieldDescriptors = reader.getDatabaseFieldsDescriptors();
    }
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    RelDataTypeFactory.Builder builder = typeFactory.builder();
    for (DBaseFieldDescriptor field : fieldDescriptors) {
      builder.add(field.getName(), typeFactory.createSqlType(sqlType(field)));
    }
    builder.add("geometry", typeFactory.createSqlType(SqlTypeName.GEOMETRY));
    return builder.build();
  }

  private static SqlTypeName sqlType(DBaseFieldDescriptor field) {
    return switch (field.getType()) {
      case CHARACTER, MEMO, PICTURE, VARI_FIELD, VARIANT -> SqlTypeName.VARCHAR;
      case NUMBER -> field.getDecimalCount() == 0 ? SqlTypeName.BIGINT : SqlTypeName.DOUBLE;
      case CURRENCY, DOUBLE -> SqlTypeName.DOUBLE;
      case INTEGER, AUTO_INCREMENT -> SqlTypeName.INTEGER;
      case LOGICAL -> SqlTypeName.BOOLEAN;
      case DATE -> SqlTypeName.DATE;
      case FLOATING_POINT -> SqlTypeName.FLOAT;
      case TIMESTAMP, DATE_TIME -> SqlTypeName.TIMESTAMP;
    };
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
    private ShapefileInputStream rows;
    private List<Object> current;

    ShapefileEnumerator(File file) {
      this.file = file;
      open();
    }

    private void open() {
      try {
        reader = new ShapefileReader(file.getPath());
        rows = reader.read();
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
        current = rows.readRow();
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
      try {
        rows.close();
        reader.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
