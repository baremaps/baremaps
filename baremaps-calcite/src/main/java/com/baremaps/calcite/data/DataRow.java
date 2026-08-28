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

import java.util.List;
import java.util.Objects;

/**
 * A row in a table with values corresponding to the schema columns.
 */
public record DataRow(DataTableSchema schema, List<Object> values) {

  /**
   * Constructs a row with validation.
   *
   * @param schema the schema
   * @param values the values
   * @throws NullPointerException if schema or values is null
   * @throws IllegalArgumentException if values size doesn't match schema columns
   */
  public DataRow {
    Objects.requireNonNull(schema, "Schema cannot be null");
    Objects.requireNonNull(values, "Values cannot be null");

    if (values.size() != schema.columns().size()) {
      throw new IllegalArgumentException(
          "Number of values (" + values.size() + ") doesn't match number of columns ("
              + schema.columns().size() + ")");
    }

    // Make a defensive copy to ensure immutability
    values = List.copyOf(values);
  }

  /**
   * Gets the value of the specified column.
   *
   * @param column the column name
   * @return the value
   * @throws IllegalArgumentException if the column is not found
   */
  public Object get(String column) {
    Objects.requireNonNull(column, "Column name cannot be null");

    // Use the schema's method to get column index for better performance
    int index = schema.getColumnIndex(column);
    return values.get(index);
  }

  /**
   * Gets the value at the specified index.
   *
   * @param index the index
   * @return the value
   * @throws IndexOutOfBoundsException if the index is out of bounds
   */
  public Object get(int index) {
    if (index < 0 || index >= values.size()) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + values.size());
    }
    return values.get(index);
  }

  /**
   * Creates a new row with the specified column set to the given value.
   *
   * @param column the column name
   * @param value the value
   * @return a new row with the updated value
   * @throws IllegalArgumentException if the column is not found
   */
  public DataRow with(String column, Object value) {
    Objects.requireNonNull(column, "Column name cannot be null");

    int index = schema.getColumnIndex(column);
    return with(index, value);
  }

  /**
   * Creates a new row with the value at the specified index set to the given value.
   *
   * @param index the index
   * @param value the value
   * @return a new row with the updated value
   * @throws IndexOutOfBoundsException if the index is out of bounds
   */
  public DataRow with(int index, Object value) {
    if (index < 0 || index >= values.size()) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + values.size());
    }

    // Validate value against column type if value is not null
    if (value != null) {
      DataColumn column = schema.columns().get(index);
      validateValue(column, value);
    }

    // Create a new list with the updated value
    var newValues = new java.util.ArrayList<>(values);
    newValues.set(index, value);
    return new DataRow(schema, newValues);
  }

  /**
   * Validates that the value is compatible with the column.
   *
   * @param column the column
   * @param value the value
   * @throws IllegalArgumentException if the value is not compatible
   */
  private void validateValue(DataColumn column, Object value) {
    // Skip validation for null values
    if (value == null) {
      if (column.isRequired()) {
        throw new IllegalArgumentException(
            "Column " + column.name() + " is required but value is null");
      }
      return;
    }

    // For non-null values, validate against the type
    Class<?> javaType = column.javaType();
    if (!javaType.isInstance(value)) {
      throw new IllegalArgumentException(
          "Value for column " + column.name() + " must be of type " + javaType.getSimpleName()
              + " but was " + value.getClass().getSimpleName());
    }
  }

  /**
   * @deprecated Use {@link #with(String, Object)} instead.
   */
  @Deprecated
  public void set(String column, Object value) {
    throw new UnsupportedOperationException("DataRow is immutable. Use 'with' methods instead.");
  }

  /**
   * @deprecated Use {@link #with(int, Object)} instead.
   */
  @Deprecated
  public void set(int index, Object value) {
    throw new UnsupportedOperationException("DataRow is immutable. Use 'with' methods instead.");
  }
}
