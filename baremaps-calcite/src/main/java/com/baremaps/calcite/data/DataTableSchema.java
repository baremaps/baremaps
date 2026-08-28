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

import com.baremaps.calcite.data.DataColumn.Cardinality;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.*;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;

/**
 * A {@link DataTableSchema} defines the structure of a table.
 */
public record DataTableSchema(String name,
    List<DataColumn> columns) implements Serializable {

  /**
   * Constructs a schema with validation.
   *
   * @param name the name of the schema
   * @param columns the columns in the schema
   * @throws NullPointerException if name or columns is null
   * @throws IllegalArgumentException if name is blank, columns is empty, or columns contains
   *         duplicates
   */
  public DataTableSchema {
    Objects.requireNonNull(name, "Schema name cannot be null");
    Objects.requireNonNull(columns, "Columns cannot be null");

    if (name.isBlank()) {
      throw new IllegalArgumentException("Schema name cannot be blank");
    }

    if (columns.isEmpty()) {
      throw new IllegalArgumentException("Columns cannot be empty");
    }

    // Check for duplicate column names
    Set<String> columnNames = new HashSet<>();
    for (DataColumn column : columns) {
      if (!columnNames.add(column.name())) {
        throw new IllegalArgumentException("Duplicate column name: " + column.name());
      }
    }

    // Make defensive copy
    columns = List.copyOf(columns);
  }

  /**
   * Creates a new row for this schema with all values set to null.
   *
   * @return a new row
   */
  public DataRow createRow() {
    var values = new ArrayList<>(columns.size());
    for (int i = 0; i < columns.size(); i++) {
      values.add(null);
    }
    return new DataRow(this, values);
  }

  /**
   * Gets a column by name.
   *
   * @param name the name of the column
   * @return the column
   * @throws IllegalArgumentException if the column does not exist
   */
  public DataColumn getColumn(String name) {
    Objects.requireNonNull(name, "Column name cannot be null");

    for (DataColumn column : columns) {
      if (column.name().equals(name)) {
        return column;
      }
    }
    throw new IllegalArgumentException("Column not found: " + name);
  }

  /**
   * Gets the index of a column by name.
   *
   * @param name the name of the column
   * @return the index of the column
   * @throws IllegalArgumentException if the column does not exist
   */
  public int getColumnIndex(String name) {
    Objects.requireNonNull(name, "Column name cannot be null");

    for (int i = 0; i < columns.size(); i++) {
      if (columns.get(i).name().equals(name)) {
        return i;
      }
    }
    throw new IllegalArgumentException("Column not found: " + name);
  }

  /**
   * Checks if a column exists.
   *
   * @param name the name of the column
   * @return true if the column exists
   */
  public boolean hasColumn(String name) {
    Objects.requireNonNull(name, "Column name cannot be null");

    for (DataColumn column : columns) {
      if (column.name().equals(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Custom JSON deserializer for DataSchema.
   */
  static class DataSchemaDeserializer extends JsonDeserializer<DataTableSchema> {
    private RelDataTypeFactory typeFactory;

    /**
     * Constructs a DataSchemaDeserializer with the given type factory.
     * 
     * @param typeFactory the type factory to use
     */
    public DataSchemaDeserializer(RelDataTypeFactory typeFactory) {
      this.typeFactory = Objects.requireNonNull(typeFactory, "Type factory cannot be null");
    }

    @Override
    public DataTableSchema deserialize(JsonParser parser, DeserializationContext ctxt)
        throws IOException {
      ObjectNode node = parser.getCodec().readTree(parser);
      if (!node.has("name")) {
        throw new IOException("Missing required field: name");
      }
      if (!node.has("columns")) {
        throw new IOException("Missing required field: columns");
      }

      String name = node.get("name").asText();
      List<DataColumn> columns = new ArrayList<>();

      JsonNode columnsNode = node.get("columns");
      if (!columnsNode.isArray()) {
        throw new IOException("columns field must be an array");
      }

      columnsNode.elements().forEachRemaining(column -> {
        try {
          columns.add(deserialize(column));
        } catch (Exception e) {
          throw new RuntimeException("Error deserializing column", e);
        }
      });

      return new DataTableSchema(name, columns);
    }

    DataColumn deserialize(JsonNode node) {
      if (!node.has("name") || !node.has("cardinality") || !node.has("sqlTypeName")) {
        throw new IllegalArgumentException(
            "Column is missing required fields: name, cardinality, or sqlTypeName");
      }

      String columnName = node.get("name").asText();
      Cardinality cardinality;
      try {
        cardinality = Cardinality.valueOf(node.get("cardinality").asText());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Invalid cardinality value: " + node.get("cardinality").asText());
      }

      SqlTypeName sqlTypeName;
      try {
        sqlTypeName = SqlTypeName.valueOf(node.get("sqlTypeName").asText());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Invalid SQL type name value: " + node.get("sqlTypeName").asText());
      }

      // Create the RelDataType based on the SqlTypeName
      RelDataType relDataType;
      if (sqlTypeName == SqlTypeName.ROW) {
        if (!node.has("columns")) {
          throw new IllegalArgumentException("Nested column is missing required field: columns");
        }

        List<DataColumn> columns = new ArrayList<>();
        JsonNode columnsNode = node.get("columns");
        if (!columnsNode.isArray()) {
          throw new IllegalArgumentException("columns field must be an array");
        }

        columnsNode.elements().forEachRemaining(column -> {
          columns.add(deserialize(column));
        });

        return DataColumnNested.of(columnName, cardinality, columns, typeFactory);
      } else {
        // Create basic type without nullability, precision, etc.
        relDataType = typeFactory.createSqlType(sqlTypeName);

        // Handle nullability based on cardinality
        if (cardinality == Cardinality.OPTIONAL) {
          relDataType = typeFactory.createTypeWithNullability(relDataType, true);
        }

        return new DataColumnFixed(columnName, cardinality, relDataType);
      }
    }
  }

  /**
   * Configures an ObjectMapper for DataSchema serialization/deserialization.
   *
   * @param typeFactory the type factory to use
   * @return a configured ObjectMapper
   */
  private static ObjectMapper configureObjectMapper(RelDataTypeFactory typeFactory) {
    var mapper = new ObjectMapper();
    mapper.registerSubtypes(
        new NamedType(DataColumnFixed.class, "FIXED"),
        new NamedType(DataColumnNested.class, "NESTED"));
    var module = new SimpleModule();
    module.addDeserializer(DataTableSchema.class, new DataSchemaDeserializer(typeFactory));
    mapper.registerModule(module);
    return mapper;
  }

  /**
   * Reads a DataSchema from an input stream.
   *
   * @param inputStream the input stream
   * @param typeFactory the type factory to use
   * @return the schema
   * @throws IOException if an I/O error occurs
   */
  public static DataTableSchema read(InputStream inputStream, RelDataTypeFactory typeFactory)
      throws IOException {
    Objects.requireNonNull(inputStream, "Input stream cannot be null");
    Objects.requireNonNull(typeFactory, "Type factory cannot be null");

    var mapper = configureObjectMapper(typeFactory);
    return mapper.readValue(inputStream, DataTableSchema.class);
  }

  /**
   * Writes a DataSchema to an output stream.
   *
   * @param outputStream the output stream
   * @param schema the schema
   * @param typeFactory the type factory to use
   * @throws IOException if an I/O error occurs
   */
  public static void write(OutputStream outputStream, DataTableSchema schema,
      RelDataTypeFactory typeFactory) throws IOException {
    Objects.requireNonNull(outputStream, "Output stream cannot be null");
    Objects.requireNonNull(schema, "Schema cannot be null");
    Objects.requireNonNull(typeFactory, "Type factory cannot be null");

    var mapper = configureObjectMapper(typeFactory);
    mapper.writeValue(outputStream, schema);
  }
}
