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

package com.baremaps.calcite.csv;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
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
 * A Calcite table implementation for CSV data. This table reads data from a CSV file and makes it
 * available through the Apache Calcite framework for SQL querying.
 */
public class CsvTable extends AbstractTable implements ScannableTable {

  private final File file;
  private final CsvSchema csvSchema;
  private RelDataType rowType;

  /**
   * Constructs a CsvTable with the specified parameters.
   *
   * @param file the CSV file to read data from
   * @param separator the separator used in the CSV file
   * @param hasHeader whether the CSV file includes a header row
   * @throws IOException if an I/O error occurs
   */
  public CsvTable(File file, char separator, boolean hasHeader) throws IOException {
    this.file = file;
    this.csvSchema = buildSchema(file, separator, hasHeader);
  }

  /**
   * Builds a CSV schema from the file.
   *
   * @param file the CSV file
   * @param separator the column separator
   * @param hasHeader whether the CSV file has a header row
   * @return the CSV schema
   * @throws IOException if an I/O error occurs
   */
  private static CsvSchema buildSchema(File file, char separator, boolean hasHeader)
      throws IOException {
    CsvMapper mapper = new CsvMapper();
    if (hasHeader) {
      // Read the header to infer column names
      CsvSchema baseSchema = CsvSchema.emptySchema()
          .withHeader()
          .withColumnSeparator(separator);
      try (MappingIterator<Map<String, String>> iterator = mapper.readerFor(Map.class)
          .with(baseSchema)
          .readValues(file)) {
        // Consume one record so that the header is processed
        if (iterator.hasNext()) {
          iterator.next();
        }
        CsvSchema inferred = (CsvSchema) iterator.getParserSchema();
        if (inferred != null) {
          return inferred;
        }
      }
      // Fallback if inference failed
      return baseSchema;
    } else {
      // No header: read the first row to determine the number of columns
      CsvSchema.Builder builder = CsvSchema.builder().setColumnSeparator(separator);
      String firstLine = Files.readAllLines(file.toPath()).get(0);
      String[] columns = firstLine.split(String.valueOf(separator));
      for (int i = 0; i < columns.length; i++) {
        builder.addColumn("column" + (i + 1));
      }
      return builder.build();
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
   * Creates the row type (schema) for the CSV data.
   *
   * @param typeFactory the type factory
   * @return the RelDataType representing the schema
   */
  private RelDataType createRowType(RelDataTypeFactory typeFactory) {
    RelDataTypeFactory.Builder builder = typeFactory.builder();

    // Define the columns based on the CSV schema
    for (String columnName : csvSchema.getColumnNames()) {
      // For simplicity, we'll treat all columns as VARCHAR
      // In a more sophisticated implementation, we could infer types from the data
      builder.add(columnName, SqlTypeName.VARCHAR);
    }

    return builder.build();
  }

  @Override
  public Enumerable<Object[]> scan(DataContext root) {
    return new AbstractEnumerable<Object[]>() {
      @Override
      public Enumerator<Object[]> enumerator() {
        return new CsvEnumerator(file, csvSchema);
      }
    };
  }

  /**
   * Enumerator for CSV data.
   */
  private static class CsvEnumerator implements Enumerator<Object[]> {
    private final File file;
    private final CsvSchema csvSchema;
    private MappingIterator<Map<String, String>> iterator;
    private Object[] current;

    public CsvEnumerator(File file, CsvSchema csvSchema) {
      this.file = file;
      this.csvSchema = csvSchema;
      initialize();
    }

    private void initialize() {
      try {
        CsvMapper mapper = new CsvMapper();
        iterator = mapper.readerFor(Map.class)
            .with(csvSchema)
            .readValues(file);
      } catch (IOException e) {
        throw new RuntimeException("Failed to initialize CSV iterator", e);
      }
    }

    @Override
    public Object[] current() {
      return current;
    }

    @Override
    public boolean moveNext() {
      try {
        if (iterator.hasNext()) {
          Map<String, String> row = iterator.next();
          current = mapToArray(row);
          return true;
        }
        return false;
      } catch (Exception e) {
        throw new RuntimeException("Error reading CSV row", e);
      }
    }

    @Override
    public void reset() {
      try {
        iterator.close();
        initialize();
      } catch (IOException e) {
        throw new RuntimeException("Failed to reset CSV iterator", e);
      }
    }

    @Override
    public void close() {
      try {
        if (iterator != null) {
          iterator.close();
        }
      } catch (IOException e) {
        // Ignore
      }
    }

    /**
     * Converts a Map to an Object array.
     *
     * @param row the CSV row as a Map
     * @return the corresponding row array
     */
    private Object[] mapToArray(Map<String, String> row) {
      List<String> columnNames = csvSchema.getColumnNames();
      Object[] result = new Object[columnNames.size()];
      for (int i = 0; i < columnNames.size(); i++) {
        String columnName = columnNames.get(i);
        result[i] = row.get(columnName);
      }
      return result;
    }
  }
}
