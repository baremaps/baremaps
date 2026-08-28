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
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Calcite table implementation for FlatGeoBuf data. This table reads data from a FlatGeoBuf file
 * and makes it available through the Apache Calcite framework for SQL querying.
 */
public class FlatGeoBufTable extends AbstractTable implements ScannableTable {

  private static final Logger logger = LoggerFactory.getLogger(FlatGeoBufTable.class);

  private final File file;
  private final String tableName;
  private final List<FlatGeoBuf.Column> columns;
  private RelDataType rowType;

  /**
   * Constructs a FlatGeoBufTable with the specified file.
   *
   * @param file the FlatGeoBuf file to read data from
   * @throws IOException if an I/O error occurs
   */
  public FlatGeoBufTable(File file) throws IOException {
    this.file = file;
    this.tableName = file.getName().replaceFirst("[.][^.]+$", ""); // Remove extension

    // Read header to get columns information
    try (FlatGeoBufReader reader =
        new FlatGeoBufReader(FileChannel.open(file.toPath(), StandardOpenOption.READ))) {
      FlatGeoBuf.Header header = reader.readHeader();
      this.columns = header.columns();
    }
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    if (rowType == null) {
      rowType = createRowType(typeFactory);
    }
    return rowType;
  }

  /**
   * Creates the row type (schema) for the FlatGeoBuf data.
   *
   * @param typeFactory the type factory
   * @return the RelDataType representing the schema
   */
  private RelDataType createRowType(RelDataTypeFactory typeFactory) {
    RelDataTypeFactory.Builder builder = typeFactory.builder();

    // Add columns from the FlatGeoBuf schema
    for (FlatGeoBuf.Column column : columns) {
      SqlTypeName sqlTypeName = mapFlatGeoBufTypeToSqlType(column.type());
      RelDataType fieldType = typeFactory.createSqlType(sqlTypeName);

      // Handle nullability
      if (column.nullable()) {
        fieldType = typeFactory.createTypeWithNullability(fieldType, true);
      }

      builder.add(column.name(), fieldType);
    }

    // Add geometry column
    builder.add("geometry", typeFactory.createJavaType(Geometry.class));

    return builder.build();
  }

  /**
   * Maps FlatGeoBuf column types to SqlTypeName
   * 
   * @param columnType the FlatGeoBuf column type
   * @return the corresponding SqlTypeName
   */
  private SqlTypeName mapFlatGeoBufTypeToSqlType(FlatGeoBuf.ColumnType columnType) {
    return switch (columnType) {
      case BYTE, UBYTE -> SqlTypeName.TINYINT;
      case BOOL -> SqlTypeName.BOOLEAN;
      case SHORT, USHORT -> SqlTypeName.SMALLINT;
      case INT, UINT -> SqlTypeName.INTEGER;
      case LONG, ULONG -> SqlTypeName.BIGINT;
      case FLOAT -> SqlTypeName.FLOAT;
      case DOUBLE -> SqlTypeName.DOUBLE;
      case STRING -> SqlTypeName.VARCHAR;
      case JSON, DATETIME -> SqlTypeName.VARCHAR;
      case BINARY -> SqlTypeName.VARBINARY;
    };
  }

  @Override
  public Enumerable<Object[]> scan(DataContext root) {
    return new AbstractEnumerable<Object[]>() {
      @Override
      public Enumerator<Object[]> enumerator() {
        return new FlatGeoBufEnumerator(file);
      }
    };
  }

  /**
   * Enumerator for FlatGeoBuf data.
   */
  private static class FlatGeoBufEnumerator implements Enumerator<Object[]> {
    private final File file;
    private FlatGeoBufReader reader;
    private Object[] current;
    private long cursor = 0;
    private long featureCount;

    public FlatGeoBufEnumerator(File file) {
      this.file = file;
      initialize();
    }

    private void initialize() {
      try {
        reader = new FlatGeoBufReader(FileChannel.open(file.toPath(), StandardOpenOption.READ));
        FlatGeoBuf.Header header = reader.readHeader();
        featureCount = header.featuresCount();
        reader.skipIndex();
      } catch (IOException e) {
        throw new RuntimeException("Failed to initialize FlatGeoBuf iterator", e);
      }
    }

    @Override
    public Object[] current() {
      return current;
    }

    @Override
    public boolean moveNext() {
      try {
        if (cursor >= featureCount) {
          return false;
        }

        FlatGeoBuf.Feature feature = reader.readFeature();
        cursor++;

        // Convert feature to row
        List<Object> values = new ArrayList<>();

        // Add properties
        values.addAll(feature.properties());

        // Add geometry
        values.add(feature.geometry());

        current = values.toArray();
        return true;
      } catch (IOException e) {
        logger.error("Error reading FlatGeoBuf row", e);
        return false;
      }
    }

    @Override
    public void reset() {
      try {
        reader.close();
        initialize();
      } catch (IOException e) {
        throw new RuntimeException("Failed to reset FlatGeoBuf iterator", e);
      }
    }

    @Override
    public void close() {
      try {
        if (reader != null) {
          reader.close();
        }
      } catch (IOException e) {
        // Ignore
      }
    }
  }
}
