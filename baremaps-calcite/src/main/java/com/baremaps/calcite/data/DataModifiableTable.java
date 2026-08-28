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
import java.lang.reflect.Type;
import java.nio.MappedByteBuffer;
import java.nio.file.Paths;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
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
import org.apache.calcite.rel.type.RelDataTypeImpl;
import org.apache.calcite.rel.type.RelProtoDataType;
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
 * A modifiable table implementation for Calcite that stores data in an AppendOnlyLog.
 */
public class DataModifiableTable extends AbstractTable implements ModifiableTable, Wrapper {

  private final String name;
  private final RelProtoDataType protoRowType;
  private final RelDataType rowType;
  private final DataTableSchema schema;
  public final DataCollection<DataRow> rows;

  /**
   * Constructs a DataModifiableTable with the specified name and prototype row type.
   *
   * @param name the name of the table
   * @param protoRowType the prototype row type
   * @param typeFactory the type factory
   */
  public DataModifiableTable(String name,
      RelProtoDataType protoRowType,
      RelDataTypeFactory typeFactory) {
    super();
    this.name = requireNonNull(name, "name");
    this.protoRowType = requireNonNull(protoRowType, "protoRowType");
    this.rowType = this.protoRowType.apply(typeFactory);

    // Create the schema
    List<DataColumn> columns = new ArrayList<>();
    rowType.getFieldList().forEach(field -> {
      String columnName = field.getName();
      RelDataType relDataType = field.getType();
      DataColumn.Cardinality columnCardinality = determineCardinality(relDataType);
      columns.add(new DataColumnFixed(columnName, columnCardinality, relDataType));
    });

    this.schema = new DataTableSchema(name, columns);

    // Create the collection
    DataRowType dataRowType = new DataRowType(schema);
    Memory<MappedByteBuffer> memory = new MemoryMappedDirectory(Paths.get(this.name));
    this.rows = AppendOnlyLog.<DataRow>builder().dataType(dataRowType).memory(memory).build();
  }

  /**
   * Constructs a DataModifiableTable with an existing schema and data collection.
   *
   * @param name the name of the table
   * @param schema the data schema
   * @param rows the data collection
   * @param typeFactory the type factory
   */
  public DataModifiableTable(String name,
      DataTableSchema schema,
      DataCollection<DataRow> rows,
      RelDataTypeFactory typeFactory) {
    super();
    this.name = requireNonNull(name, "name");
    this.schema = requireNonNull(schema, "schema");
    this.rows = requireNonNull(rows, "rows");

    // Create row type from schema
    List<RelDataType> fieldTypes = new ArrayList<>();
    List<String> fieldNames = new ArrayList<>();

    for (DataColumn column : schema.columns()) {
      fieldNames.add(column.name());
      fieldTypes.add(column.relDataType());
    }

    this.rowType = typeFactory.createStructType(fieldTypes, fieldNames);
    this.protoRowType = RelDataTypeImpl.proto(rowType);
  }

  /**
   * Determines the cardinality from a RelDataType.
   *
   * @param columnType the Calcite RelDataType
   * @return the corresponding DataColumn.Cardinality
   */
  private DataColumn.Cardinality determineCardinality(RelDataType columnType) {
    Objects.requireNonNull(columnType, "Column type cannot be null");

    if (columnType.getSqlTypeName() == SqlTypeName.ARRAY) {
      return DataColumn.Cardinality.REPEATED;
    } else if (columnType.isNullable()) {
      return DataColumn.Cardinality.OPTIONAL;
    } else {
      return DataColumn.Cardinality.REQUIRED;
    }
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
  public Collection getModifiableCollection() {
    return new RowCollectionAdapter();
  }

  @Override
  public <T> Queryable<T> asQueryable(QueryProvider queryProvider, SchemaPlus schema,
      String tableName) {
    return new AbstractTableQueryable<T>(queryProvider, schema, this,
        tableName) {
      @Override
      public Enumerator<T> enumerator() {
        return (Enumerator<T>) Linq4j.enumerator(new RowCollectionAdapter());
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

  @Override
  public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
    return rowType;
  }

  /**
   * Adapter that makes the data collection appear as a collection of Object arrays. This provides
   * compatibility with Calcite's ModifiableTable interface.
   */
  private class RowCollectionAdapter extends AbstractCollection<Object[]> {

    private final int size;

    public RowCollectionAdapter() {
      this.size = (int) Math.min(rows.size(), Integer.MAX_VALUE);
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    public boolean isEmpty() {
      return rows.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      if (!(o instanceof Object[])) {
        return false;
      }
      return rows.stream().anyMatch(row -> {
        Object[] values = row.values().toArray();
        Object[] other = (Object[]) o;
        if (values.length != other.length) {
          return false;
        }
        for (int i = 0; i < values.length; i++) {
          if (!Objects.equals(values[i], other[i])) {
            return false;
          }
        }
        return true;
      });
    }

    @Override
    public Iterator<Object[]> iterator() {
      return rows.stream().map(row -> row.values().toArray()).iterator();
    }

    @Override
    public Object[] toArray() {
      return rows.stream().map(row -> row.values().toArray()).toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return (T[]) rows.stream().map(row -> row.values().toArray()).toArray();
    }

    @Override
    public boolean add(Object[] objects) {
      Objects.requireNonNull(objects, "Values cannot be null");
      if (objects.length != schema.columns().size()) {
        throw new IllegalArgumentException(
            "Expected " + schema.columns().size() + " values, got " + objects.length);
      }
      return rows.add(new DataRow(schema, List.of(objects)));
    }

    @Override
    public boolean addAll(Collection<? extends Object[]> c) {
      Objects.requireNonNull(c, "Collection cannot be null");
      return rows.addAll(c.stream()
          .map(objects -> {
            Objects.requireNonNull(objects, "Values cannot be null");
            if (objects.length != schema.columns().size()) {
              throw new IllegalArgumentException(
                  "Expected " + schema.columns().size() + " values, got " + objects.length);
            }
            return new DataRow(schema, List.of(objects));
          })
          .toList());
    }

    @Override
    public void clear() {
      rows.clear();
    }
  }
}
