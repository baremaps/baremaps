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
 * A Calcite table implementation for shapefile data. This table reads data from a shapefile and
 * makes it available through the Apache Calcite framework for SQL querying.
 */
public class ShapefileTable extends AbstractTable implements ScannableTable {

  private static final Logger logger = LoggerFactory.getLogger(ShapefileTable.class);

  private final File file;
  private final List<DBaseFieldDescriptor> fieldDescriptors;
  private RelDataType rowType;

  /**
   * Constructs a ShapefileTable with the specified file.
   *
   * @param file the shapefile to read data from
   * @throws IOException if an I/O error occurs
   */
  public ShapefileTable(File file) throws IOException {
    this.file = file;
    // Create a ShapefileReader to get field descriptors
    try (ShapefileReader shapeFile = new ShapefileReader(file.getPath())) {
      this.fieldDescriptors = shapeFile.getDatabaseFieldsDescriptors();
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
   * Creates the row type (schema) for the shapefile data.
   *
   * @param typeFactory the type factory
   * @return the RelDataType representing the schema
   */
  private RelDataType createRowType(RelDataTypeFactory typeFactory) {
    RelDataTypeFactory.Builder builder = typeFactory.builder();

    // Add columns from the shapefile's database fields
    for (DBaseFieldDescriptor fieldDescriptor : fieldDescriptors) {
      String columnName = fieldDescriptor.getName();
      RelDataType fieldType = mapDBaseTypeToSqlType(typeFactory, fieldDescriptor);
      builder.add(columnName, fieldType);
    }

    // Add geometry column
    builder.add("geometry", typeFactory.createJavaType(Geometry.class));

    return builder.build();
  }

  /**
   * Maps DBase field types to Calcite SQL types.
   *
   * @param typeFactory the type factory
   * @param fieldDescriptor the DBase field descriptor
   * @return the corresponding RelDataType
   */
  private RelDataType mapDBaseTypeToSqlType(RelDataTypeFactory typeFactory,
      DBaseFieldDescriptor fieldDescriptor) {
    return switch (fieldDescriptor.getType()) {
      case CHARACTER -> typeFactory.createSqlType(SqlTypeName.VARCHAR);
      case NUMBER -> fieldDescriptor.getDecimalCount() == 0
          ? typeFactory.createSqlType(SqlTypeName.BIGINT)
          : typeFactory.createSqlType(SqlTypeName.DOUBLE);
      case CURRENCY -> typeFactory.createSqlType(SqlTypeName.DOUBLE);
      case DOUBLE -> typeFactory.createSqlType(SqlTypeName.DOUBLE);
      case INTEGER -> typeFactory.createSqlType(SqlTypeName.INTEGER);
      case AUTO_INCREMENT -> typeFactory.createSqlType(SqlTypeName.INTEGER);
      case LOGICAL -> typeFactory.createSqlType(SqlTypeName.BOOLEAN);
      case DATE -> typeFactory.createSqlType(SqlTypeName.DATE);
      case MEMO -> typeFactory.createSqlType(SqlTypeName.VARCHAR);
      case FLOATING_POINT -> typeFactory.createSqlType(SqlTypeName.FLOAT);
      case PICTURE -> typeFactory.createSqlType(SqlTypeName.VARCHAR);
      case VARI_FIELD -> typeFactory.createSqlType(SqlTypeName.VARCHAR);
      case VARIANT -> typeFactory.createSqlType(SqlTypeName.VARCHAR);
      case TIMESTAMP -> typeFactory.createSqlType(SqlTypeName.TIMESTAMP);
      case DATE_TIME -> typeFactory.createSqlType(SqlTypeName.TIMESTAMP);
    };
  }

  @Override
  public Enumerable<Object[]> scan(DataContext root) {
    return new AbstractEnumerable<Object[]>() {
      @Override
      public Enumerator<Object[]> enumerator() {
        return new ShapefileEnumerator(file);
      }
    };
  }

  /**
   * Enumerator for shapefile data.
   */
  private static class ShapefileEnumerator implements Enumerator<Object[]> {
    private final File file;
    private ShapefileReader shapeFile;
    private ShapefileInputStream shapefileInputStream;
    private List<Object> current;

    public ShapefileEnumerator(File file) {
      this.file = file;
      initialize();
    }

    private void initialize() {
      try {
        this.shapeFile = new ShapefileReader(file.getPath());
        this.shapefileInputStream = shapeFile.read();
      } catch (IOException e) {
        throw new RuntimeException("Failed to initialize shapefile iterator", e);
      }
    }

    @Override
    public Object[] current() {
      if (current == null) {
        return null;
      }
      return current.toArray();
    }

    @Override
    public boolean moveNext() {
      try {
        current = shapefileInputStream.readRow();
        return current != null;
      } catch (IOException e) {
        logger.error("Error reading shapefile row", e);
        return false;
      }
    }

    @Override
    public void reset() {
      try {
        closeResources();
        initialize();
      } catch (IOException e) {
        throw new RuntimeException("Failed to reset shapefile iterator", e);
      }
    }

    @Override
    public void close() {
      try {
        closeResources();
      } catch (IOException e) {
        logger.error("Error closing shapefile resources", e);
      }
    }

    private void closeResources() throws IOException {
      if (shapefileInputStream != null) {
        shapefileInputStream.close();
        shapefileInputStream = null;
      }
      if (shapeFile != null) {
        shapeFile.close();
        shapeFile = null;
      }
      current = null;
    }
  }
}
