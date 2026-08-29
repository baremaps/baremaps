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

package com.baremaps.calcite.geoparquet;

import com.baremaps.geoparquet.GeoParquetGroup;
import com.baremaps.geoparquet.GeoParquetReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.hadoop.fs.Path;

/** A read-only table over a GeoParquet file. */
public class GeoParquetTable extends AbstractTable implements ScannableTable {

  private final Path path;
  private final RelDataType rowType;

  public GeoParquetTable(File file, RelDataTypeFactory typeFactory) throws IOException {
    this.path = new Path(file.toURI());
    try (GeoParquetReader reader = new GeoParquetReader(path)) {
      this.rowType = GeoParquetTypeConversion.toRelDataType(typeFactory,
          reader.getGeoParquetSchema());
    }
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    return rowType;
  }

  @Override
  public Enumerable<Object[]> scan(DataContext root) {
    return new AbstractEnumerable<>() {
      @Override
      public Enumerator<Object[]> enumerator() {
        return new GeoParquetEnumerator(path);
      }
    };
  }

  private static class GeoParquetEnumerator implements Enumerator<Object[]> {

    private final Path path;
    private GeoParquetReader reader;
    private Iterator<GeoParquetGroup> groups;
    private GeoParquetGroup current;

    GeoParquetEnumerator(Path path) {
      this.path = path;
      open();
    }

    private void open() {
      reader = new GeoParquetReader(path);
      groups = reader.read().iterator();
    }

    @Override
    public Object[] current() {
      return current == null ? null : GeoParquetTypeConversion.toRow(current);
    }

    @Override
    public boolean moveNext() {
      if (groups.hasNext()) {
        current = groups.next();
        return true;
      }
      current = null;
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
        reader.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
