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

package com.baremaps.calcite.flatgeobuf;

import com.baremaps.flatgeobuf.FlatGeoBuf;
import com.baremaps.flatgeobuf.FlatGeoBufReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
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
 * A read-only table over a FlatGeoBuf file: its properties followed by a {@code geometry} column.
 */
public class FlatGeoBufTable extends AbstractTable implements ScannableTable {

  private final File file;
  private final List<FlatGeoBuf.Column> columns;

  public FlatGeoBufTable(File file) throws IOException {
    this.file = file;
    try (FlatGeoBufReader reader = open(file)) {
      this.columns = reader.readHeader().columns();
    }
  }

  private static FlatGeoBufReader open(File file) throws IOException {
    return new FlatGeoBufReader(FileChannel.open(file.toPath(), StandardOpenOption.READ));
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    RelDataTypeFactory.Builder builder = typeFactory.builder();
    for (FlatGeoBuf.Column column : columns) {
      RelDataType type = typeFactory.createSqlType(sqlType(column.type()));
      builder.add(column.name(), typeFactory.createTypeWithNullability(type, column.nullable()));
    }
    builder.add("geometry", typeFactory.createSqlType(SqlTypeName.GEOMETRY));
    return builder.build();
  }

  private static SqlTypeName sqlType(FlatGeoBuf.ColumnType columnType) {
    return switch (columnType) {
      case BYTE, UBYTE -> SqlTypeName.TINYINT;
      case BOOL -> SqlTypeName.BOOLEAN;
      case SHORT, USHORT -> SqlTypeName.SMALLINT;
      case INT, UINT -> SqlTypeName.INTEGER;
      case LONG, ULONG -> SqlTypeName.BIGINT;
      case FLOAT -> SqlTypeName.FLOAT;
      case DOUBLE -> SqlTypeName.DOUBLE;
      case STRING, JSON, DATETIME -> SqlTypeName.VARCHAR;
      case BINARY -> SqlTypeName.VARBINARY;
    };
  }

  @Override
  public Enumerable<Object[]> scan(DataContext root) {
    return new AbstractEnumerable<>() {
      @Override
      public Enumerator<Object[]> enumerator() {
        return new FlatGeoBufEnumerator(file);
      }
    };
  }

  private static class FlatGeoBufEnumerator implements Enumerator<Object[]> {

    private final File file;
    private FlatGeoBufReader reader;
    private long remaining;
    private Object[] current;

    FlatGeoBufEnumerator(File file) {
      this.file = file;
      open();
    }

    private void open() {
      try {
        reader = FlatGeoBufTable.open(file);
        remaining = reader.readHeader().featuresCount();
        reader.skipIndex();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public Object[] current() {
      return current;
    }

    @Override
    public boolean moveNext() {
      if (remaining <= 0) {
        return false;
      }
      try {
        FlatGeoBuf.Feature feature = reader.readFeature();
        remaining--;
        List<Object> values = new ArrayList<>(feature.properties());
        values.add(feature.geometry());
        current = values.toArray();
        return true;
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
        reader.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
