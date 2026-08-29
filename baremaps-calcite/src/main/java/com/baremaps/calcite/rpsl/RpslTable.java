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

package com.baremaps.calcite.rpsl;

import com.baremaps.rpsl.RpslObject;
import com.baremaps.rpsl.RpslReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Iterator;
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
 * A read-only table over an RPSL file. Every object becomes a row with a fixed set of attribute
 * columns; attributes that may occur several times are exposed as {@code VARCHAR} arrays.
 */
public class RpslTable extends AbstractTable implements ScannableTable {

  private record Column(String name, boolean repeated) {
  }

  private static final List<Column> COLUMNS = List.of(
      new Column("type", false),
      new Column("id", false),
      new Column("inetnum", false),
      new Column("inet6num", false),
      new Column("netname", false),
      new Column("descr", true),
      new Column("country", false),
      new Column("admin-c", false),
      new Column("tech-c", false),
      new Column("status", false),
      new Column("mnt-by", false),
      new Column("created", false),
      new Column("last-modified", false),
      new Column("changed", true));

  private final File file;

  public RpslTable(File file) throws IOException {
    if (!file.isFile()) {
      throw new IOException("Not a file: " + file);
    }
    this.file = file;
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    RelDataTypeFactory.Builder builder = typeFactory.builder();
    RelDataType varchar = typeFactory.createSqlType(SqlTypeName.VARCHAR);
    for (Column column : COLUMNS) {
      builder.add(column.name(),
          column.repeated() ? typeFactory.createArrayType(varchar, -1) : varchar);
    }
    return builder.build();
  }

  @Override
  public Enumerable<Object[]> scan(DataContext root) {
    return new AbstractEnumerable<>() {
      @Override
      public Enumerator<Object[]> enumerator() {
        return new RpslEnumerator(file);
      }
    };
  }

  private static class RpslEnumerator implements Enumerator<Object[]> {

    private final File file;
    private InputStream inputStream;
    private Iterator<RpslObject> objects;
    private RpslObject current;

    RpslEnumerator(File file) {
      this.file = file;
      open();
    }

    private void open() {
      try {
        inputStream = new FileInputStream(file);
        objects = new RpslReader().read(inputStream).iterator();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public Object[] current() {
      if (current == null) {
        return null;
      }
      Object[] row = new Object[COLUMNS.size()];
      for (int i = 0; i < COLUMNS.size(); i++) {
        Column column = COLUMNS.get(i);
        row[i] = switch (column.name()) {
          case "type" -> current.type();
          case "id" -> current.id();
          default -> column.repeated()
              ? nullIfEmpty(current.all(column.name()))
              : current.first(column.name()).orElse(null);
        };
      }
      return row;
    }

    private static List<String> nullIfEmpty(List<String> values) {
      return values.isEmpty() ? null : values;
    }

    @Override
    public boolean moveNext() {
      if (objects.hasNext()) {
        current = objects.next();
        return true;
      }
      return false;
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
