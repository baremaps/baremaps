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

package com.baremaps.calcite.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelDataTypeFieldImpl;
import org.apache.calcite.rel.type.RelRecordType;
import org.apache.calcite.sql.type.SqlTypeName;

/**
 * A nested column in a table with a complex structure.
 */
public record DataColumnNested(
    String name,
    Cardinality cardinality,
    RelDataType relDataType,
    List<DataColumn> columns) implements DataColumn {

  /**
   * Constructs a nested column with validation.
   *
   * @param name the name of the column
   * @param cardinality the cardinality of the column
   * @param relDataType the RelDataType of the column
   * @param columns the nested columns
   * @throws NullPointerException if any parameter is null
   * @throws IllegalArgumentException if name is blank or columns is empty
   */
  public DataColumnNested {
    Objects.requireNonNull(name, "Column name cannot be null");
    Objects.requireNonNull(cardinality, "Column cardinality cannot be null");
    Objects.requireNonNull(relDataType, "RelDataType cannot be null");
    Objects.requireNonNull(columns, "Nested columns cannot be null");

    if (name.isBlank()) {
      throw new IllegalArgumentException("Column name cannot be blank");
    }

    if (columns.isEmpty()) {
      throw new IllegalArgumentException("Nested columns cannot be empty");
    }

    // Defensive copy to ensure immutability
    columns = List.copyOf(columns);
  }

  /**
   * Constructs a nested column from name, cardinality, and columns, inferring the RelDataType from
   * the columns.
   * 
   * @param name the name
   * @param cardinality the cardinality
   * @param columns the columns
   * @param typeFactory the type factory
   * @return a new DataColumnNested
   */
  public static DataColumnNested of(
      String name,
      Cardinality cardinality,
      List<DataColumn> columns,
      RelDataTypeFactory typeFactory) {

    Objects.requireNonNull(typeFactory, "TypeFactory cannot be null");

    // Create record type from columns
    List<RelDataTypeField> fields = new ArrayList<>();
    for (int i = 0; i < columns.size(); i++) {
      DataColumn column = columns.get(i);
      fields.add(new RelDataTypeFieldImpl(
          column.name(),
          i, // ordinal
          column.relDataType()));
    }
    RelDataType rowType = new RelRecordType(fields);

    return new DataColumnNested(name, cardinality, rowType, columns);
  }

  @Override
  public SqlTypeName sqlTypeName() {
    return SqlTypeName.ROW;
  }

  @Override
  public Class<?> javaType() {
    return Map.class;
  }
}
