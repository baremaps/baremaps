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

import com.baremaps.calcite.data.DataColumn;
import com.baremaps.calcite.data.DataColumnFixed;
import com.baremaps.calcite.data.DataTableSchema;
import com.baremaps.postgres.copy.*;
import com.baremaps.postgres.metadata.ColumnResult;
import com.baremaps.postgres.metadata.DatabaseMetadata;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.apache.calcite.DataContext;
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
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.ModifiableTable;
import org.apache.calcite.schema.QueryableTable;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Schemas;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.schema.impl.AbstractTableQueryable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.postgresql.PGConnection;
import org.postgresql.copy.PGCopyOutputStream;

/**
 * A Calcite table implementation for PostGIS. This table allows querying and modifying spatial data
 * directly from a PostGIS-enabled PostgreSQL database.
 */
public class PostgresModifiableTable extends AbstractTable
    implements ScannableTable, ModifiableTable, QueryableTable {

  private final DataSource dataSource;
  private final String schema;
  private final String tableName;
  private final RelDataType rowType;
  private final DataTableSchema dataTableSchema;

  /**
   * Constructs a PostgisTable with the specified data source and table name.
   *
   * @param dataSource the data source for the PostgreSQL connection
   * @param tableName the name of the table to access
   * @throws SQLException if an SQL error occurs
   */
  public PostgresModifiableTable(DataSource dataSource, String tableName) throws SQLException {
    this(dataSource, "public", tableName, new org.apache.calcite.jdbc.JavaTypeFactoryImpl());
  }

  /**
   * Constructs a PostgisTable with the specified data source, table name, and type factory.
   *
   * @param dataSource the data source for the PostgreSQL connection
   * @param tableName the name of the table to access
   * @param typeFactory the type factory
   * @throws SQLException if an SQL error occurs
   */
  public PostgresModifiableTable(DataSource dataSource, String tableName,
      RelDataTypeFactory typeFactory)
      throws SQLException {
    this(dataSource, "public", tableName, typeFactory);
  }

  /**
   * Constructs a PostgisTable with the specified data source, schema, table name, and type factory.
   *
   * @param dataSource the data source for the PostgreSQL connection
   * @param schema the schema name
   * @param tableName the name of the table to access
   * @param typeFactory the type factory
   * @throws SQLException if an SQL error occurs
   */
  public PostgresModifiableTable(DataSource dataSource, String schema, String tableName,
      RelDataTypeFactory typeFactory)
      throws SQLException {
    this.dataSource = dataSource;
    this.schema = schema;
    this.tableName = tableName;
    this.dataTableSchema = discoverSchema();
    this.rowType = PostgresTypeConversion.toRelDataType(typeFactory, dataTableSchema);
  }

  /**
   * Discovers the schema of the PostGIS table using DatabaseMetadata.
   *
   * @return the schema of the table
   * @throws SQLException if an SQL error occurs
   */
  private DataTableSchema discoverSchema() throws SQLException {
    List<DataColumn> columns = new ArrayList<>();

    // Use DatabaseMetadata to get column information
    DatabaseMetadata metadata = new DatabaseMetadata(dataSource);
    var tableMetadata =
        metadata.getTableMetaData(null, schema, tableName, new String[] {"TABLE", "VIEW"})
            .stream()
            .filter(meta -> meta.table().tableName().equalsIgnoreCase(tableName))
            .findFirst();

    // If not found, check if it's a materialized view
    if (tableMetadata.isEmpty()) {
      try (Connection connection = dataSource.getConnection();
          PreparedStatement stmt = connection.prepareStatement(
              "SELECT EXISTS (SELECT 1 FROM pg_matviews WHERE schemaname = ? AND matviewname = ?)")) {
        stmt.setString(1, schema);
        stmt.setString(2, tableName);
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next() && rs.getBoolean(1)) {
            // It's a materialized view, get column information directly
            return getSchemaFromDirectQuery();
          }
        }
      }

      // If we get here, it's neither a regular table/view nor a materialized view
      throw new SQLException("Table not found: " + schema + "." + tableName);
    }

    // Get geometry column types for the current table.
    Map<String, String> geometryTypes = getGeometryTypes();

    for (ColumnResult column : tableMetadata.get().columns()) {
      String columnName = column.columnName();
      String dataType = column.typeName();
      boolean isNullable = "YES".equalsIgnoreCase(column.isNullable());

      // Determine column cardinality
      DataColumn.Cardinality cardinality =
          isNullable ? DataColumn.Cardinality.OPTIONAL : DataColumn.Cardinality.REQUIRED;

      // Create a data column based on the type
      RelDataTypeFactory typeFactory = new org.apache.calcite.jdbc.JavaTypeFactoryImpl();
      RelDataType relDataType;

      if ("geometry".equalsIgnoreCase(dataType) && geometryTypes.containsKey(columnName)) {
        // This is a geometry column
        relDataType = typeFactory.createSqlType(SqlTypeName.GEOMETRY);
      } else {
        // Handle regular PostgreSQL types
        relDataType = PostgresTypeConversion.postgresTypeToRelDataType(
            typeFactory, dataType);
      }

      columns.add(new DataColumnFixed(columnName, cardinality, relDataType));
    }

    return new DataTableSchema(tableName, columns);
  }

  /**
   * Gets schema information directly from a query rather than metadata, which is useful for objects
   * like materialized views that aren't captured by standard metadata.
   *
   * @return the schema constructed from direct column query
   * @throws SQLException if an SQL error occurs
   */
  private DataTableSchema getSchemaFromDirectQuery() throws SQLException {
    List<DataColumn> columns = new ArrayList<>();

    try (Connection connection = dataSource.getConnection()) {
      // First try with pg_catalog, which works for both tables and materialized views
      String sql =
          "SELECT a.attname AS column_name, " +
              "       pg_catalog.format_type(a.atttypid, a.atttypmod) AS data_type, " +
              "       NOT a.attnotnull AS is_nullable " +
              "FROM pg_catalog.pg_attribute a " +
              "JOIN pg_catalog.pg_class c ON a.attrelid = c.oid " +
              "LEFT JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace " +
              "WHERE n.nspname = ? " +
              "  AND c.relname = ? " +
              "  AND a.attnum > 0 " +
              "  AND NOT a.attisdropped " +
              "ORDER BY a.attnum";

      try (PreparedStatement stmt = connection.prepareStatement(sql)) {
        stmt.setString(1, schema);
        stmt.setString(2, tableName);

        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            String columnName = rs.getString("column_name");
            String dataType = rs.getString("data_type");
            boolean isNullable = rs.getBoolean("is_nullable");

            // Determine column cardinality
            DataColumn.Cardinality cardinality =
                isNullable ? DataColumn.Cardinality.OPTIONAL : DataColumn.Cardinality.REQUIRED;

            // Create a data column based on the type
            RelDataTypeFactory typeFactory = new org.apache.calcite.jdbc.JavaTypeFactoryImpl();
            RelDataType relDataType;

            // Check if it's a geometry column by looking at the data type
            if (dataType.contains("geometry")) {
              relDataType = typeFactory.createSqlType(SqlTypeName.GEOMETRY);
            } else {
              // Map PostgreSQL type to Calcite type
              relDataType = PostgresTypeConversion.postgresTypeToRelDataType(
                  typeFactory, mapPostgresTypeName(dataType));
            }

            columns.add(new DataColumnFixed(columnName, cardinality, relDataType));
          }
        }
      }

      // If we didn't find any columns, try falling back to information_schema.columns
      if (columns.isEmpty()) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT column_name, data_type, is_nullable " +
                "FROM information_schema.columns " +
                "WHERE table_name = ? " +
                "ORDER BY ordinal_position")) {

          stmt.setString(1, tableName);

          try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
              String columnName = rs.getString("column_name");
              String dataType = rs.getString("data_type");
              boolean isNullable = "YES".equalsIgnoreCase(rs.getString("is_nullable"));

              // Determine column cardinality
              DataColumn.Cardinality cardinality =
                  isNullable ? DataColumn.Cardinality.OPTIONAL : DataColumn.Cardinality.REQUIRED;

              // Create a data column based on the type
              RelDataTypeFactory typeFactory = new org.apache.calcite.jdbc.JavaTypeFactoryImpl();
              RelDataType relDataType;

              // Check if it's a geometry column
              if ("USER-DEFINED".equals(dataType)) {
                // Check if this is a geometry column by querying for spatial_ref_sys
                try (PreparedStatement geometryCheck = connection.prepareStatement(
                    "SELECT type FROM geometry_columns " +
                        "WHERE f_table_name = ? AND f_geometry_column = ?")) {
                  geometryCheck.setString(1, tableName);
                  geometryCheck.setString(2, columnName);
                  try (ResultSet geomRs = geometryCheck.executeQuery()) {
                    if (geomRs.next()) {
                      relDataType = typeFactory.createSqlType(SqlTypeName.GEOMETRY);
                    } else {
                      // Not a geometry, handle as regular type
                      relDataType = PostgresTypeConversion.postgresTypeToRelDataType(
                          typeFactory, dataType);
                    }
                  }
                }
              } else {
                // Regular PostgreSQL type
                relDataType = PostgresTypeConversion.postgresTypeToRelDataType(
                    typeFactory, dataType);
              }

              columns.add(new DataColumnFixed(columnName, cardinality, relDataType));
            }
          }
        }
      }
    }

    if (columns.isEmpty()) {
      throw new SQLException("No columns found for table: " + schema + "." + tableName);
    }

    return new DataTableSchema(tableName, columns);
  }

  /**
   * Maps PostgreSQL type name from pg_catalog format to a simpler format that can be used with
   * PostgresTypeConversion.
   *
   * @param pgTypeName the PostgreSQL type name from pg_catalog
   * @return simplified type name
   */
  private String mapPostgresTypeName(String pgTypeName) {
    if (pgTypeName == null) {
      return "unknown";
    }

    // Strip size/precision information
    if (pgTypeName.contains("(")) {
      pgTypeName = pgTypeName.substring(0, pgTypeName.indexOf("("));
    }

    // Map common types
    switch (pgTypeName.toLowerCase()) {
      case "int4":
        return "integer";
      case "int8":
        return "bigint";
      case "int2":
        return "smallint";
      case "float4":
        return "real";
      case "float8":
        return "double precision";
      case "varchar":
      case "character varying":
        return "varchar";
      case "bpchar":
      case "character":
        return "char";
      case "text":
        return "text";
      case "bool":
        return "boolean";
      case "timestamptz":
        return "timestamp with time zone";
      case "timestamp":
        return "timestamp without time zone";
      case "date":
        return "date";
      case "time":
        return "time";
      case "timetz":
        return "time with time zone";
      case "numeric":
      case "decimal":
        return "numeric";
      default:
        return pgTypeName;
    }
  }

  /**
   * Gets geometry column types for the current table.
   *
   * @return a map of column names to geometry types
   * @throws SQLException if an SQL error occurs
   */
  private Map<String, String> getGeometryTypes() throws SQLException {
    Map<String, String> geometryTypes = new HashMap<>();

    try (Connection connection = dataSource.getConnection()) {
      // Query to get geometry column information
      String sql =
          "SELECT f_geometry_column, type FROM geometry_columns WHERE f_table_schema = ? AND f_table_name = ?";

      try (PreparedStatement stmt = connection.prepareStatement(sql)) {
        stmt.setString(1, schema);
        stmt.setString(2, tableName);

        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            String column = rs.getString("f_geometry_column");
            String type = rs.getString("type");
            geometryTypes.put(column, type);
          }
        }
      }
    }

    return geometryTypes;
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    return rowType;
  }

  /**
   * Returns the schema of this table.
   *
   * @return the schema of the table
   */
  public DataTableSchema schema() {
    return dataTableSchema;
  }

  /**
   * Returns the data source used by this table.
   *
   * @return the data source
   */
  protected DataSource getDataSource() {
    return dataSource;
  }

  /**
   * Returns the name of this table.
   *
   * @return the table name
   */
  protected String getTableName() {
    return tableName;
  }

  /**
   * Returns the schema name of this table.
   *
   * @return the schema name
   */
  protected String getSchema() {
    return schema;
  }

  @Override
  public Enumerable<Object[]> scan(DataContext root) {
    return new AbstractEnumerable<>() {
      @Override
      public Enumerator<Object[]> enumerator() {
        return new PostgisEnumerator(dataSource, dataTableSchema, schema, tableName);
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
  public Collection getModifiableCollection() {
    return new PostgisCollectionAdapter();
  }

  @Override
  public <T> Queryable<T> asQueryable(QueryProvider queryProvider, SchemaPlus schema,
      String tableName) {
    return new AbstractTableQueryable<T>(queryProvider, schema, this,
        tableName) {
      @Override
      public Enumerator<T> enumerator() {
        return (Enumerator<T>) Linq4j.enumerator(new PostgisCollectionAdapter());
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
   * Adapter that makes the PostGIS table appear as a collection of Object arrays. This provides
   * compatibility with Calcite's ModifiableTable interface.
   */
  private class PostgisCollectionAdapter extends AbstractCollection<Object[]> {

    @Override
    public int size() {
      try (Connection connection = dataSource.getConnection();
          Statement stmt = connection.createStatement();
          ResultSet rs =
              stmt.executeQuery("SELECT COUNT(*) FROM \"" + schema + "\".\"" + tableName + "\"")) {
        if (rs.next()) {
          return rs.getInt(1);
        }
        return 0;
      } catch (SQLException e) {
        throw new RuntimeException("Error getting table size", e);
      }
    }

    @Override
    public boolean isEmpty() {
      return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
      if (!(o instanceof Object[])) {
        return false;
      }

      // This is a simplified implementation - in a real-world scenario,
      // you might want to implement a more efficient way to check for existence
      try (Connection connection = dataSource.getConnection();
          Statement stmt = connection.createStatement()) {

        Object[] values = (Object[]) o;
        StringBuilder whereClause = new StringBuilder();

        for (int i = 0; i < dataTableSchema.columns().size(); i++) {
          if (i > 0) {
            whereClause.append(" AND ");
          }
          whereClause.append("\"").append(dataTableSchema.columns().get(i).name()).append("\" = ?");
        }

        String sql =
            "SELECT COUNT(*) FROM \"" + schema + "\".\"" + tableName + "\" WHERE " + whereClause;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
          for (int i = 0; i < values.length; i++) {
            statement.setObject(i + 1, values[i]);
          }
          try (ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
              return rs.getInt(1) > 0;
            }
          }
        }
        return false;
      } catch (SQLException e) {
        throw new RuntimeException("Error checking if row exists", e);
      }
    }

    @Override
    public Iterator<Object[]> iterator() {
      return new Iterator<Object[]>() {
        private final PostgisEnumerator enumerator =
            new PostgisEnumerator(dataSource, dataTableSchema, schema, tableName);
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
          return current;
        }
      };
    }

    @Override
    public Object[] toArray() {
      List<Object[]> result = new ArrayList<>();
      try (Connection connection = dataSource.getConnection();
          Statement stmt = connection.createStatement();
          ResultSet rs =
              stmt.executeQuery("SELECT * FROM \"" + schema + "\".\"" + tableName + "\"")) {

        while (rs.next()) {
          Object[] row = new Object[dataTableSchema.columns().size()];
          for (int i = 0; i < dataTableSchema.columns().size(); i++) {
            DataColumn column = dataTableSchema.columns().get(i);
            if (column.sqlTypeName() == SqlTypeName.GEOMETRY) {
              byte[] wkb = rs.getBytes(i + 1);
              row[i] = deserializeWkb(wkb);
            } else {
              row[i] = rs.getObject(i + 1);
            }
          }
          result.add(row);
        }
      } catch (SQLException e) {
        throw new RuntimeException("Error converting table to array", e);
      }
      return result.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return (T[]) toArray();
    }

    @Override
    public boolean add(Object[] objects) {
      return addAll(Collections.singletonList(objects));
    }

    @Override
    public boolean addAll(Collection<? extends Object[]> c) {
      Objects.requireNonNull(c, "Collection cannot be null");

      try (Connection connection = dataSource.getConnection()) {
        // Use COPY API for better performance
        PGConnection pgConnection = connection.unwrap(PGConnection.class);
        String copyCommand = "COPY \"" + schema + "\".\"" + tableName + "\" (" +
            dataTableSchema.columns().stream()
                .map(col -> "\"" + col.name() + "\"")
                .collect(java.util.stream.Collectors.joining(", "))
            +
            ") FROM STDIN WITH (FORMAT binary)";

        try (
            CopyWriter writer = new CopyWriter(new PGCopyOutputStream(pgConnection, copyCommand))) {
          writer.writeHeader();

          for (Object[] objects : c) {
            Objects.requireNonNull(objects, "Values cannot be null");
            if (objects.length != dataTableSchema.columns().size()) {
              throw new IllegalArgumentException(
                  "Expected " + dataTableSchema.columns().size() + " values, got "
                      + objects.length);
            }

            writer.startRow(dataTableSchema.columns().size());

            for (int i = 0; i < objects.length; i++) {
              Object value = objects[i];
              DataColumn column = dataTableSchema.columns().get(i);

              if (value == null) {
                writer.writeNull();
              } else if (column.sqlTypeName() == SqlTypeName.GEOMETRY
                  && value instanceof Geometry) {
                writer.write(CopyWriter.GEOMETRY_HANDLER, (Geometry) value);
              } else if (column.sqlTypeName() == SqlTypeName.BOOLEAN) {
                writer.writeBoolean((Boolean) value);
              } else if (column.sqlTypeName() == SqlTypeName.INTEGER) {
                writer.writeInteger((Integer) value);
              } else if (column.sqlTypeName() == SqlTypeName.BIGINT) {
                writer.writeLong((Long) value);
              } else if (column.sqlTypeName() == SqlTypeName.DOUBLE) {
                writer.writeDouble((Double) value);
              } else if (column.sqlTypeName() == SqlTypeName.FLOAT) {
                writer.writeFloat((Float) value);
              } else if (column.sqlTypeName() == SqlTypeName.SMALLINT) {
                writer.writeShort((Short) value);
              } else if (column.sqlTypeName() == SqlTypeName.TINYINT) {
                writer.writeByte((Byte) value);
              } else if (column.sqlTypeName() == SqlTypeName.DATE) {
                writer.write(CopyWriter.LOCAL_DATE_HANDLER, (java.time.LocalDate) value);
              } else if (column.sqlTypeName() == SqlTypeName.TIMESTAMP) {
                writer.write(CopyWriter.LOCAL_DATE_TIME_HANDLER, (java.time.LocalDateTime) value);
              } else if (column.sqlTypeName() == SqlTypeName.OTHER) {
                writer.writeJsonb(value.toString());
              } else {
                // For other types, convert to string
                writer.write(value.toString());
              }
            }
          }

          return true;
        }
      } catch (Exception e) {
        throw new RuntimeException("Error adding rows using COPY API", e);
      }
    }

    @Override
    public void clear() {
      try (Connection connection = dataSource.getConnection();
          Statement stmt = connection.createStatement()) {
        stmt.executeUpdate("DELETE FROM \"" + schema + "\".\"" + tableName + "\"");
      } catch (SQLException e) {
        throw new RuntimeException("Error clearing table", e);
      }
    }

    /**
     * Deserializes a WKB (Well-Known Binary) representation of a geometry.
     *
     * @param wkb the WKB bytes
     * @return the deserialized geometry
     */
    private Geometry deserializeWkb(byte[] wkb) {
      if (wkb == null) {
        return null;
      }
      try {
        // Create a WKBReader that handles SRID information
        WKBReader reader = new WKBReader(new GeometryFactory());
        // The WKB data is already in the correct format, no need to skip bytes
        return reader.read(wkb);
      } catch (ParseException e) {
        throw new RuntimeException("Error parsing WKB geometry", e);
      }
    }

    /**
     * Serializes a geometry to WKB (Well-Known Binary) format.
     *
     * @param geometry the geometry to serialize
     * @return the WKB bytes
     */
    private byte[] serializeWkb(Geometry geometry) {
      if (geometry == null) {
        return null;
      }
      try {
        org.locationtech.jts.io.WKBWriter writer = new org.locationtech.jts.io.WKBWriter();
        return writer.write(geometry);
      } catch (Exception e) {
        throw new RuntimeException("Error serializing geometry to WKB", e);
      }
    }
  }

  /**
   * Enumerator for PostGIS data.
   */
  private static class PostgisEnumerator implements Enumerator<Object[]> {
    private final DataSource dataSource;
    private final DataTableSchema schema;
    private final String tableSchema;
    private final String tableName;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private boolean hasNext;
    private Object[] current;

    /**
     * Constructs a PostgisEnumerator with the specified data source and schema.
     *
     * @param dataSource the data source
     * @param schema the schema
     */
    public PostgisEnumerator(DataSource dataSource, DataTableSchema schema, String tableSchema,
        String tableName) {
      this.dataSource = dataSource;
      this.schema = schema;
      this.tableSchema = tableSchema;
      this.tableName = tableName;
      this.current = null;
      try {
        this.connection = dataSource.getConnection();
        this.statement = connection.createStatement();
        this.resultSet = statement.executeQuery(buildSelectQuery());
        this.hasNext = resultSet.next();
        if (this.hasNext) {
          this.current = convertCurrentRow();
        }
      } catch (SQLException e) {
        close();
        throw new RuntimeException("Error initializing PostGIS query", e);
      }
    }

    private String buildSelectQuery() {
      List<String> columnProjections = new ArrayList<>();

      for (DataColumn column : schema.columns()) {
        // Special handling for geometry columns to get WKB format
        if (column.sqlTypeName() == SqlTypeName.GEOMETRY) {
          columnProjections.add(String.format("ST_AsBinary(\"%s\") AS \"%s\"",
              column.name(), column.name()));
        } else {
          columnProjections.add(String.format("\"%s\"", column.name()));
        }
      }

      return "SELECT " + String.join(", ", columnProjections) +
          " FROM \"" + tableSchema + "\".\"" + tableName + "\"";
    }

    private Object[] convertCurrentRow() throws SQLException {
      Object[] values = new Object[schema.columns().size()];
      for (int i = 0; i < schema.columns().size(); i++) {
        DataColumn column = schema.columns().get(i);
        Object value;

        // Special handling for geometry columns
        if (column.sqlTypeName() == SqlTypeName.GEOMETRY) {
          byte[] wkb = resultSet.getBytes(i + 1);
          value = deserializeWkb(wkb);
        } else {
          value = resultSet.getObject(i + 1);
        }

        values[i] = value;
      }
      return values;
    }

    private Geometry deserializeWkb(byte[] wkb) {
      if (wkb == null) {
        return null;
      }
      try {
        // Create a WKBReader that handles SRID information
        WKBReader reader = new WKBReader(new GeometryFactory());
        // The WKB data is already in the correct format, no need to skip bytes
        return reader.read(wkb);
      } catch (ParseException e) {
        throw new RuntimeException("Error parsing WKB geometry", e);
      }
    }

    @Override
    public Object[] current() {
      return current;
    }

    @Override
    public boolean moveNext() {
      try {
        if (!hasNext) {
          current = null;
          return false;
        }

        current = convertCurrentRow();
        hasNext = resultSet.next();
        return true;
      } catch (SQLException e) {
        close();
        throw new RuntimeException("Error fetching next result", e);
      }
    }

    @Override
    public void reset() {
      close();
      try {
        this.connection = dataSource.getConnection();
        this.statement = connection.createStatement();
        this.resultSet = statement.executeQuery(buildSelectQuery());
        this.hasNext = resultSet.next();
        if (this.hasNext) {
          this.current = convertCurrentRow();
        }
      } catch (SQLException e) {
        close();
        throw new RuntimeException("Error resetting PostGIS query", e);
      }
    }

    @Override
    public void close() {
      try {
        if (resultSet != null) {
          resultSet.close();
          resultSet = null;
        }
        if (statement != null) {
          statement.close();
          statement = null;
        }
        if (connection != null) {
          connection.close();
          connection = null;
        }
      } catch (SQLException e) {
        throw new RuntimeException("Error closing resources", e);
      }
    }
  }
}
