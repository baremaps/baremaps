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

package com.baremaps.calcite.postgres;

import com.baremaps.postgres.copy.CopyWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.calcite.DataContext;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
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
import org.apache.calcite.schema.QueryableTable;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Schemas;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.schema.impl.AbstractTableQueryable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.postgresql.PGConnection;
import org.postgresql.copy.PGCopyOutputStream;

/**
 * A table backed by a PostgreSQL table (or view, or materialized view). Reads stream the rows with
 * a plain {@code SELECT}; writes go through the binary {@code COPY} protocol.
 */
public class PostgresModifiableTable extends AbstractTable
    implements ScannableTable, ModifiableTable, QueryableTable {

  private final DataSource dataSource;
  private final String schema;
  private final String tableName;
  private final RelDataType rowType;

  /** Opens a table of the {@code public} schema. */
  public PostgresModifiableTable(DataSource dataSource, String tableName) throws SQLException {
    this(dataSource, "public", tableName, new JavaTypeFactoryImpl());
  }

  /** @throws SQLException if the table does not exist or the catalog cannot be read */
  public PostgresModifiableTable(DataSource dataSource, String schema, String tableName,
      RelDataTypeFactory typeFactory) throws SQLException {
    this.dataSource = dataSource;
    this.schema = schema;
    this.tableName = tableName;
    this.rowType = PostgresCatalog.rowType(dataSource, schema, tableName, typeFactory);
  }

  String qualifiedName() {
    return "\"" + schema + "\".\"" + tableName + "\"";
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
        return new PostgresEnumerator(dataSource, qualifiedName(), rowType);
      }
    };
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

  /**
   * The {@link Collection} view of the rows that Calcite mutates.
   *
   * <p>
   * Calcite inserts by calling {@link #add} once per row, so a plain {@code INSERT ... SELECT}
   * opens one {@code COPY} per row; callers with many rows should batch them through
   * {@link #addAll}.
   */
  private class RowCollection extends AbstractCollection<Object[]> {

    @Override
    public int size() {
      try (Connection connection = dataSource.getConnection();
          Statement statement = connection.createStatement();
          ResultSet resultSet =
              statement.executeQuery("SELECT COUNT(*) FROM " + qualifiedName())) {
        return resultSet.next() ? resultSet.getInt(1) : 0;
      } catch (SQLException e) {
        throw new IllegalStateException("Failed to count the rows of " + qualifiedName(), e);
      }
    }

    @Override
    public Iterator<Object[]> iterator() {
      PostgresEnumerator enumerator = new PostgresEnumerator(dataSource, qualifiedName(), rowType);
      return new Iterator<>() {
        private boolean hasNext = enumerator.moveNext();

        @Override
        public boolean hasNext() {
          return hasNext;
        }

        @Override
        public Object[] next() {
          if (!hasNext) {
            throw new NoSuchElementException();
          }
          Object[] current = enumerator.current();
          hasNext = enumerator.moveNext();
          if (!hasNext) {
            enumerator.close();
          }
          return current;
        }
      };
    }

    @Override
    public boolean add(Object[] row) {
      return addAll(Collections.singletonList(row));
    }

    @Override
    public boolean addAll(Collection<? extends Object[]> rows) {
      List<RelDataTypeField> fields = rowType.getFieldList();
      String copy = "COPY " + qualifiedName() + " ("
          + fields.stream().map(f -> "\"" + f.getName() + "\"").collect(Collectors.joining(", "))
          + ") FROM STDIN WITH (FORMAT binary)";
      try (Connection connection = dataSource.getConnection();
          CopyWriter writer = new CopyWriter(
              new PGCopyOutputStream(connection.unwrap(PGConnection.class), copy))) {
        writer.writeHeader();
        for (Object[] row : rows) {
          if (row.length != fields.size()) {
            throw new IllegalArgumentException(
                "Expected " + fields.size() + " values, got " + row.length);
          }
          writer.startRow(fields.size());
          for (int i = 0; i < row.length; i++) {
            write(writer, fields.get(i), row[i]);
          }
        }
        return true;
      } catch (SQLException | IOException e) {
        throw new IllegalStateException("Failed to copy rows into " + qualifiedName(), e);
      }
    }

    private void write(CopyWriter writer, RelDataTypeField field, Object value)
        throws IOException {
      if (value == null) {
        writer.writeNull();
        return;
      }
      switch (field.getType().getSqlTypeName()) {
        case GEOMETRY -> writer.write(CopyWriter.GEOMETRY_HANDLER, (Geometry) value);
        case BOOLEAN -> writer.writeBoolean((Boolean) value);
        case TINYINT -> writer.writeByte((Byte) value);
        case SMALLINT -> writer.writeShort((Short) value);
        case INTEGER -> writer.writeInteger((Integer) value);
        case BIGINT -> writer.writeLong((Long) value);
        case FLOAT, REAL -> writer.writeFloat((Float) value);
        case DOUBLE, DECIMAL -> writer.writeDouble(((Number) value).doubleValue());
        case DATE -> writer.write(CopyWriter.LOCAL_DATE_HANDLER, (LocalDate) value);
        case TIMESTAMP -> writer.write(CopyWriter.LOCAL_DATE_TIME_HANDLER, (LocalDateTime) value);
        case OTHER -> writer.writeJsonb(value.toString());
        default -> writer.write(value.toString());
      }
    }

    @Override
    public void clear() {
      try (Connection connection = dataSource.getConnection();
          Statement statement = connection.createStatement()) {
        statement.executeUpdate("DELETE FROM " + qualifiedName());
      } catch (SQLException e) {
        throw new IllegalStateException("Failed to clear " + qualifiedName(), e);
      }
    }
  }
}
