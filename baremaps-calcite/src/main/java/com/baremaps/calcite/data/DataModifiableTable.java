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

import static java.util.Objects.requireNonNull;

import com.baremaps.data.collection.AppendOnlyLog;
import com.baremaps.data.collection.DataCollection;
import com.baremaps.data.memory.Memory;
import com.baremaps.data.memory.MemoryMappedDirectory;
import com.baremaps.data.memory.OnHeapMemory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.linq4j.Linq4j;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.linq4j.Queryable;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.prepare.Prepare;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.TableModify;
import org.apache.calcite.rel.logical.LogicalTableModify;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.ModifiableTable;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Schemas;
import org.apache.calcite.schema.Wrapper;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.schema.impl.AbstractTableQueryable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A modifiable table whose rows live in a {@code baremaps-data} collection, either on the heap or
 * in a memory-mapped directory.
 *
 * <p>
 * A memory-mapped table is self-describing: its row type is stored as JSON in the header of the
 * memory, after the bytes that {@link AppendOnlyLog} reserves for itself, so that
 * {@link #open(Path, RelDataTypeFactory)} can reopen it without any side file.
 */
public class DataModifiableTable extends AbstractTable
    implements ModifiableTable, Wrapper, AutoCloseable {

  private static final int SCHEMA_OFFSET = AppendOnlyLog.HEADER_BYTES;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String name;
  private final RelDataType rowType;
  private final DataCollection<Object[]> rows;

  /** Creates a table over an existing collection of rows matching {@code rowType}. */
  public DataModifiableTable(String name, RelDataType rowType, DataCollection<Object[]> rows) {
    this.name = requireNonNull(name, "name");
    this.rowType = requireNonNull(rowType, "rowType");
    this.rows = requireNonNull(rows, "rows");
  }

  /** Creates an empty table held on the heap. */
  public static DataModifiableTable inMemory(String name, RelDataType rowType) {
    return new DataModifiableTable(name, rowType, log(rowType, new OnHeapMemory()));
  }

  /** Creates an empty table in {@code directory}, which must be empty or absent. */
  public static DataModifiableTable create(Path directory, String name, RelDataType rowType) {
    Memory<?> memory = new MemoryMappedDirectory(directory);
    writeSchema(memory.header(), rowType);
    return new DataModifiableTable(name, rowType, log(rowType, memory));
  }

  /** Opens the table previously created in {@code directory}; its name is the directory name. */
  public static DataModifiableTable open(Path directory, RelDataTypeFactory typeFactory) {
    Memory<?> memory = new MemoryMappedDirectory(directory);
    RelDataType rowType = readSchema(memory.header(), typeFactory);
    String name = directory.getFileName().toString();
    return new DataModifiableTable(name, rowType, log(rowType, memory));
  }

  private static DataCollection<Object[]> log(RelDataType rowType, Memory<?> memory) {
    return new AppendOnlyLog<>(new DataRowType(rowType), memory);
  }

  private static void writeSchema(ByteBuffer header, RelDataType rowType) {
    ArrayNode columns = MAPPER.createArrayNode();
    for (RelDataTypeField field : rowType.getFieldList()) {
      columns.addObject()
          .put("name", field.getName())
          .put("type", field.getType().getSqlTypeName().name())
          .put("nullable", field.getType().isNullable());
    }
    try {
      byte[] bytes = MAPPER.writeValueAsBytes(columns);
      header.putInt(SCHEMA_OFFSET, bytes.length);
      header.put(SCHEMA_OFFSET + Integer.BYTES, bytes);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static RelDataType readSchema(ByteBuffer header, RelDataTypeFactory typeFactory) {
    int length = header.getInt(SCHEMA_OFFSET);
    byte[] bytes = new byte[length];
    header.get(SCHEMA_OFFSET + Integer.BYTES, bytes);
    RelDataTypeFactory.Builder builder = typeFactory.builder();
    try {
      for (JsonNode column : MAPPER.readTree(bytes)) {
        ObjectNode node = (ObjectNode) column;
        RelDataType type =
            typeFactory.createSqlType(SqlTypeName.valueOf(node.get("type").asText()));
        type = typeFactory.createTypeWithNullability(type, node.get("nullable").asBoolean());
        builder.add(node.get("name").asText(), type);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return builder.build();
  }

  public String name() {
    return name;
  }

  public DataCollection<Object[]> rows() {
    return rows;
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    return rowType;
  }

  @Override
  public TableModify toModificationRel(
      RelOptCluster cluster,
      RelOptTable table,
      Prepare.CatalogReader catalogReader,
      RelNode child,
      TableModify.Operation operation,
      @Nullable List<String> updateColumnList,
      @Nullable List<RexNode> sourceExpressionList,
      boolean flattened) {
    return LogicalTableModify.create(table, catalogReader, child, operation,
        updateColumnList, sourceExpressionList, flattened);
  }

  @Override
  public Collection<Object[]> getModifiableCollection() {
    return new RowCollection();
  }

  @Override
  public <T> Queryable<T> asQueryable(QueryProvider queryProvider, SchemaPlus schema,
      String tableName) {
    return new AbstractTableQueryable<>(queryProvider, schema, this, tableName) {
      @Override
      @SuppressWarnings("unchecked")
      public Enumerator<T> enumerator() {
        return (Enumerator<T>) Linq4j.enumerator(new RowCollection());
      }
    };
  }

  @Override
  public Type getElementType() {
    return Object[].class;
  }

  @Override
  public Expression getExpression(SchemaPlus schema, String tableName, Class clazz) {
    return Schemas.tableExpression(schema, getElementType(), tableName, clazz);
  }

  /** Persists the row count of a memory-mapped table. */
  @Override
  public void close() throws Exception {
    rows.close();
  }

  /** The {@link Collection} view of the rows that Calcite mutates. */
  private class RowCollection extends AbstractCollection<Object[]> {

    @Override
    public int size() {
      return (int) Math.min(rows.size(), Integer.MAX_VALUE);
    }

    @Override
    public Iterator<Object[]> iterator() {
      return rows.iterator();
    }

    @Override
    public boolean add(Object[] row) {
      if (row.length != rowType.getFieldCount()) {
        throw new IllegalArgumentException(
            "Expected " + rowType.getFieldCount() + " values, got " + row.length);
      }
      return rows.add(row);
    }

    @Override
    public void clear() {
      rows.clear();
    }
  }
}
