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
import com.baremaps.geoparquet.GeoParquetSchema;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.hadoop.fs.Path;

/**
 * A Calcite table implementation for GeoParquet data. This table reads data from a GeoParquet file
 * and makes it available through the Apache Calcite framework for SQL querying.
 */
public class GeoParquetTable extends AbstractTable implements ScannableTable {

  private final File file;
  private final RelDataType rowType;
  private final GeoParquetSchema geoParquetSchema;

  /**
   * Constructs a GeoParquetTable with the specified file.
   *
   * @param file the GeoParquet file to read data from
   * @throws IOException if an I/O error occurs
   */
  public GeoParquetTable(File file) throws IOException {
    this(file, new org.apache.calcite.jdbc.JavaTypeFactoryImpl());
  }

  /**
   * Constructs a GeoParquetTable with the specified file and type factory.
   *
   * @param file the GeoParquet file to read data from
   * @param typeFactory the type factory
   * @throws IOException if an I/O error occurs
   */
  public GeoParquetTable(File file, RelDataTypeFactory typeFactory) throws IOException {
    this.file = file;
    try (GeoParquetReader reader = new GeoParquetReader(new Path(file.toURI()))) {
      this.geoParquetSchema = reader.getGeoParquetSchema();
      this.rowType = GeoParquetTypeConversion.toRelDataType(typeFactory, this.geoParquetSchema);
    } catch (IOException e) {
      throw new IOException("Failed to read GeoParquet file", e);
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
        try {
          return new GeoParquetEnumerator(
              new GeoParquetReader(new Path(file.toURI())),
              geoParquetSchema);
        } catch (Exception e) {
          throw new RuntimeException("Failed to create GeoParquet enumerator", e);
        }
      }
    };
  }

  /**
   * Enumerator for GeoParquet data.
   */
  private static class GeoParquetEnumerator implements Enumerator<Object[]> {

    private final GeoParquetReader reader;
    private final GeoParquetSchema schema;
    private GeoParquetGroup currentRow;
    private boolean hasNext;
    private final Path path;
    private java.util.Iterator<GeoParquetGroup> iterator;

    public GeoParquetEnumerator(GeoParquetReader reader, GeoParquetSchema schema) {
      this.reader = reader;
      this.schema = schema;
      this.path = new Path(reader.getGeoParquetSchema().name());
      try {
        // Initialize the iterator once from the stream
        this.iterator = reader.read().iterator();
      } catch (Exception e) {
        throw new RuntimeException("Failed to initialize GeoParquet reader", e);
      }
      this.hasNext = true;
      moveNext();
    }

    @Override
    public Object[] current() {
      if (currentRow == null) {
        return new Object[0];
      }
      List<Object> values = GeoParquetTypeConversion.asPostgresRowValues(currentRow);
      return values.toArray();
    }

    @Override
    public boolean moveNext() {
      if (!hasNext) {
        return false;
      }

      if (iterator.hasNext()) {
        currentRow = iterator.next();
        return true;
      } else {
        currentRow = null;
        hasNext = false;
        return false;
      }
    }

    @Override
    public void reset() {
      try {
        reader.close();
        GeoParquetReader newReader = new GeoParquetReader(this.path);
        this.iterator = newReader.read().iterator();
        currentRow = null;
        hasNext = true;
        moveNext();
      } catch (IOException e) {
        throw new RuntimeException("Failed to reset GeoParquet reader", e);
      }
    }

    @Override
    public void close() {
      try {
        reader.close();
      } catch (IOException e) {
        throw new RuntimeException("Failed to close GeoParquet reader", e);
      }
    }
  }
}
