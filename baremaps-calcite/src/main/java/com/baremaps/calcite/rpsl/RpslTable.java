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
import java.util.ArrayList;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Calcite table implementation for RPSL data. This table reads data from a RPSL file and makes it
 * available through the Apache Calcite framework for SQL querying.
 */
public class RpslTable extends AbstractTable implements ScannableTable {

  private static final Logger logger = LoggerFactory.getLogger(RpslTable.class);

  private final File file;
  private final String tableName;
  private final List<RpslColumn> columns;
  private RelDataType rowType;

  /**
   * Represents a column in the RPSL table.
   */
  private static class RpslColumn {
    private final String name;
    private final boolean repeated;

    public RpslColumn(String name, boolean repeated) {
      this.name = name;
      this.repeated = repeated;
    }

    public String getName() {
      return name;
    }

    public boolean isRepeated() {
      return repeated;
    }
  }

  /**
   * Constructs a RpslTable with the specified file.
   *
   * @param file the RPSL file to read data from
   * @throws IOException if an I/O error occurs
   */
  public RpslTable(File file) throws IOException {
    this.file = file;
    this.tableName = file.getName();
    this.columns = buildColumns();
  }

  /**
   * Builds the columns for the RPSL data.
   *
   * @return the list of columns
   */
  private List<RpslColumn> buildColumns() {
    var columns = new ArrayList<RpslColumn>();

    // Add standard RPSL columns
    columns.add(new RpslColumn("type", false));
    columns.add(new RpslColumn("id", false));
    columns.add(new RpslColumn("inetnum", false));
    columns.add(new RpslColumn("inet6num", false));
    columns.add(new RpslColumn("netname", false));
    columns.add(new RpslColumn("descr", true));
    columns.add(new RpslColumn("country", false));
    columns.add(new RpslColumn("admin-c", false));
    columns.add(new RpslColumn("tech-c", false));
    columns.add(new RpslColumn("status", false));
    columns.add(new RpslColumn("mnt-by", false));
    columns.add(new RpslColumn("created", false));
    columns.add(new RpslColumn("last-modified", false));
    columns.add(new RpslColumn("changed", true));

    return columns;
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    if (rowType == null) {
      rowType = createRowType(typeFactory);
    }
    return rowType;
  }

  /**
   * Creates the row type (schema) for the RPSL data.
   *
   * @param typeFactory the type factory
   * @return the RelDataType representing the schema
   */
  private RelDataType createRowType(RelDataTypeFactory typeFactory) {
    RelDataTypeFactory.Builder builder = typeFactory.builder();

    // Define the columns based on the schema
    for (RpslColumn column : columns) {
      if (column.isRepeated()) {
        // For repeated fields, use a list type
        builder.add(column.getName(),
            typeFactory.createArrayType(typeFactory.createSqlType(SqlTypeName.VARCHAR), -1));
      } else {
        builder.add(column.getName(), typeFactory.createSqlType(SqlTypeName.VARCHAR));
      }
    }

    return builder.build();
  }

  @Override
  public Enumerable<Object[]> scan(DataContext root) {
    return new AbstractEnumerable<Object[]>() {
      @Override
      public Enumerator<Object[]> enumerator() {
        return new RpslEnumerator(file, columns);
      }
    };
  }

  /**
   * Enumerator for RPSL data.
   */
  private static class RpslEnumerator implements Enumerator<Object[]> {
    private final File file;
    private final List<RpslColumn> columns;
    private InputStream inputStream;
    private Iterator<RpslObject> rpslObjectIterator;
    private RpslObject current;

    public RpslEnumerator(File file, List<RpslColumn> columns) {
      this.file = file;
      this.columns = columns;
      initialize();
    }

    private void initialize() {
      try {
        this.inputStream = new FileInputStream(file);
        RpslReader rpslReader = new RpslReader();
        this.rpslObjectIterator = rpslReader.read(inputStream).iterator();
      } catch (IOException e) {
        throw new RuntimeException("Failed to initialize RPSL iterator", e);
      }
    }

    @Override
    public Object[] current() {
      if (current == null) {
        return null;
      }
      return createRow(current);
    }

    @Override
    public boolean moveNext() {
      if (rpslObjectIterator.hasNext()) {
        current = rpslObjectIterator.next();
        return true;
      }
      return false;
    }

    @Override
    public void reset() {
      try {
        if (inputStream != null) {
          inputStream.close();
        }
        initialize();
      } catch (IOException e) {
        throw new RuntimeException("Failed to reset RPSL iterator", e);
      }
    }

    @Override
    public void close() {
      try {
        if (inputStream != null) {
          inputStream.close();
        }
      } catch (IOException e) {
        // Ignore
      }
    }

    private Object[] createRow(RpslObject rpslObject) {
      Object[] row = new Object[columns.size()];

      for (int i = 0; i < columns.size(); i++) {
        RpslColumn column = columns.get(i);
        String columnName = column.getName().toLowerCase();

        if (columnName.equals("type")) {
          row[i] = rpslObject.type();
        } else if (columnName.equals("id")) {
          row[i] = rpslObject.id();
        } else {
          if (column.isRepeated()) {
            List<String> values = rpslObject.all(columnName);
            row[i] = values.isEmpty() ? null : values;
          } else {
            row[i] = rpslObject.first(columnName).orElse(null);
          }
        }
      }

      return row;
    }
  }
}
