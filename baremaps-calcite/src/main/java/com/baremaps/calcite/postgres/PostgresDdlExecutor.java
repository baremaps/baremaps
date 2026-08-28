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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.apache.calcite.util.Static.RESOURCE;

import com.baremaps.calcite.ddl.*;
import com.baremaps.calcite.sql.BaremapsSqlDdlParser;
import com.google.common.collect.ImmutableList;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import javax.sql.DataSource;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.avatica.AvaticaUtils;
import org.apache.calcite.jdbc.CalcitePrepare;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.ContextSqlValidator;
import org.apache.calcite.linq4j.Ord;
import org.apache.calcite.materialize.MaterializationKey;
import org.apache.calcite.materialize.MaterializationService;
import org.apache.calcite.model.JsonSchema;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.schema.*;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.schema.impl.ViewTable;
import org.apache.calcite.schema.impl.ViewTableMacro;
import org.apache.calcite.server.DdlExecutor;
import org.apache.calcite.server.DdlExecutorImpl;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.dialect.CalciteSqlDialect;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlAbstractParserImpl;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParserImplFactory;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.pretty.SqlPrettyWriter;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.tools.*;
import org.apache.calcite.util.NlsString;
import org.apache.calcite.util.Pair;
import org.apache.calcite.util.Util;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Executes DDL commands for PostgreSQL tables.
 *
 * <p>
 * Given a DDL command that is a sub-class of {@link SqlNode}, dispatches the command to an
 * appropriate {@code execute} method. For example, "CREATE TABLE" ({@link SqlCreateTable}) is
 * dispatched to {@link #execute(SqlCreateTable, CalcitePrepare.Context)}.
 */
public class PostgresDdlExecutor extends DdlExecutorImpl {
  /** Singleton instance. */
  public static final PostgresDdlExecutor INSTANCE = new PostgresDdlExecutor();

  /** Thread-local storage for DataSource when running through parser factory. */
  private static final ThreadLocal<DataSource> THREAD_LOCAL_DATASOURCE = new ThreadLocal<>();

  /** Data source for PostgreSQL database connection. */
  private final DataSource dataSource;

  /** Parser factory. */
  @SuppressWarnings("unused") // used via reflection
  public static final SqlParserImplFactory PARSER_FACTORY =
      new SqlParserImplFactory() {
        @Override
        public SqlAbstractParserImpl getParser(Reader stream) {
          return BaremapsSqlDdlParser.FACTORY.getParser(stream);
        }

        @Override
        public DdlExecutor getDdlExecutor() {
          return PostgresDdlExecutor.INSTANCE;
        }
      };

  /**
   * Record to hold schema information.
   */
  private record SchemaInfo(String name, @Nullable CalciteSchema schema) {
  }

  /**
   * Default constructor that assumes a DataSource is provided by PostgresSchemaFactory or similar.
   * Protected only to allow sub-classing; use {@link #INSTANCE} where possible.
   */
  protected PostgresDdlExecutor() {
    // This constructor is for singleton usage
    this.dataSource = null;
  }

  /**
   * Constructor for PostgresDdlExecutor with a specified DataSource.
   * 
   * @param dataSource the data source for PostgreSQL connection
   */
  public PostgresDdlExecutor(DataSource dataSource) {
    this.dataSource = requireNonNull(dataSource, "dataSource");
  }

  /**
   * Sets the thread-local DataSource to be used by the singleton INSTANCE. This is useful for
   * testing or when the same thread will be used for the entire operation. The DataSource will be
   * used only for the current thread.
   * 
   * @param dataSource the DataSource to use for the current thread
   */
  public static void setThreadLocalDataSource(DataSource dataSource) {
    THREAD_LOCAL_DATASOURCE.set(requireNonNull(dataSource, "dataSource"));
  }

  /**
   * Clears the thread-local DataSource after use.
   */
  public static void clearThreadLocalDataSource() {
    THREAD_LOCAL_DATASOURCE.remove();
  }

  /**
   * Returns the schema in which to create an object; the left part is null if the schema does not
   * exist.
   */
  static SchemaInfo schema(
      CalcitePrepare.Context context, boolean mutable, SqlIdentifier id) {
    final String name;
    final List<String> path;
    if (id.isSimple()) {
      path = context.getDefaultSchemaPath();
      name = id.getSimple();
    } else {
      path = Util.skipLast(id.names);
      name = Util.last(id.names);
    }
    CalciteSchema schema =
        mutable ? context.getMutableRootSchema()
            : context.getRootSchema();
    for (String p : path) {
      @Nullable
      CalciteSchema subSchema = schema.getSubSchema(p, true);
      if (subSchema == null) {
        return new SchemaInfo(name, null);
      }
      schema = subSchema;
    }
    return new SchemaInfo(name, schema);
  }

  /**
   * Returns the SqlValidator with the given {@code context} schema and type factory.
   */
  static SqlValidator validator(CalcitePrepare.Context context,
      boolean mutable) {
    return new ContextSqlValidator(context, mutable);
  }

  /**
   * Wraps a query to rename its columns. Used by CREATE VIEW and CREATE MATERIALIZED VIEW.
   */
  static SqlNode renameColumns(@Nullable SqlNodeList columnList,
      SqlNode query) {
    if (columnList == null) {
      return query;
    }
    final SqlParserPos p = query.getParserPosition();
    final SqlNodeList selectList = SqlNodeList.SINGLETON_STAR;
    final SqlCall from =
        SqlStdOperatorTable.AS.createCall(p,
            ImmutableList.<SqlNode>builder()
                .add(query)
                .add(new SqlIdentifier("_", p))
                .addAll(columnList)
                .build());
    return new SqlSelect(p, null, selectList, from, null, null, null, null,
        null, null, null, null, null);
  }

  /**
   * Gets the DataSource for PostgreSQL connections.
   * 
   * @param context the context which may contain a DataSource
   * @return the DataSource to use for PostgreSQL operations
   * @throws IllegalStateException if no DataSource is available
   */
  private DataSource getDataSource(CalcitePrepare.Context context) {
    if (dataSource != null) {
      return dataSource;
    }

    // Try thread-local DataSource first
    DataSource threadLocalDs = THREAD_LOCAL_DATASOURCE.get();
    if (threadLocalDs != null) {
      return threadLocalDs;
    }

    // Try to get DataSource from context or other sources
    // This would need to be implemented based on how you store/retrieve the DataSource
    throw new IllegalStateException("No DataSource available for PostgreSQL operations");
  }

  /** Truncate the PostgreSQL table. */
  static void truncate(SqlIdentifier name, CalcitePrepare.Context context, DataSource dataSource) {
    final SchemaInfo schemaInfo = schema(context, true, name);
    final String tableName = schemaInfo.name();
    try (Connection connection = dataSource.getConnection();
        PreparedStatement stmt =
            connection.prepareStatement("TRUNCATE TABLE \"" + tableName + "\"")) {
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Error truncating table: " + tableName, e);
    }
  }

  /**
   * Returns the value of a literal, converting {@link NlsString} into String.
   */
  @SuppressWarnings("rawtypes")
  static @Nullable Comparable value(SqlNode node) {
    final Comparable v = SqlLiteral.value(node);
    return v instanceof NlsString ? ((NlsString) v).getValue() : v;
  }

  /** Populates the table called {@code name} by executing {@code query}. */
  static void populate(SqlIdentifier name, SqlNode query,
      CalcitePrepare.Context context) {
    // Generate, prepare and execute an "INSERT INTO table query" statement.
    // (It's a bit inefficient that we convert from SqlNode to SQL and back
    // again.)
    final FrameworkConfig config = Frameworks.newConfigBuilder()
        .defaultSchema(context.getRootSchema().plus())
        .build();
    final Planner planner = Frameworks.getPlanner(config);
    try {
      final StringBuilder buf = new StringBuilder();
      final SqlWriterConfig writerConfig =
          SqlPrettyWriter.config().withAlwaysUseParentheses(false);
      final SqlPrettyWriter w = new SqlPrettyWriter(writerConfig, buf);
      buf.append("INSERT INTO ");
      name.unparse(w, 0, 0);
      buf.append(' ');
      query.unparse(w, 0, 0);
      final String sql = buf.toString();
      final SqlNode query1 = planner.parse(sql);
      final SqlNode query2 = planner.validate(query1);
      final RelRoot r = planner.rel(query2);
      final PreparedStatement prepare =
          context.getRelRunner().prepareStatement(r.rel);
      int rowCount = prepare.executeUpdate();
      Util.discard(rowCount);
      prepare.close();
    } catch (SqlParseException | ValidationException
        | RelConversionException | SQLException e) {
      throw Util.throwAsRuntime(e);
    }
  }

  /** Executes a {@code CREATE FOREIGN SCHEMA} command. */
  public void execute(SqlCreateForeignSchema create,
      CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo =
        schema(context, true, create.name);
    requireNonNull(schemaInfo.schema()); // TODO: should not assume parent schema exists
    if (schemaInfo.schema().plus().getSubSchema(schemaInfo.name()) != null) {
      if (!create.getReplace() && !create.ifNotExists) {
        throw SqlUtil.newContextException(create.name.getParserPosition(),
            RESOURCE.schemaExists(schemaInfo.name()));
      }
    }
    final Schema subSchema;
    final String libraryName;
    if (create.type != null) {
      checkArgument(create.library == null);
      final String typeName = (String) requireNonNull(value(create.type));
      final JsonSchema.Type type =
          Util.enumVal(JsonSchema.Type.class,
              typeName.toUpperCase(Locale.ROOT));
      if (type != null) {
        switch (type) {
          case JDBC:
            libraryName = JdbcSchema.Factory.class.getName();
            break;
          default:
            libraryName = null;
        }
      } else {
        libraryName = null;
      }
      if (libraryName == null) {
        throw SqlUtil.newContextException(create.type.getParserPosition(),
            RESOURCE.schemaInvalidType(typeName,
                Arrays.toString(JsonSchema.Type.values())));
      }
    } else {
      libraryName =
          requireNonNull((String) value(requireNonNull(create.library)));
    }
    final SchemaFactory schemaFactory =
        AvaticaUtils.instantiatePlugin(SchemaFactory.class, libraryName);
    final Map<String, Object> operandMap = new LinkedHashMap<>();
    for (Pair<SqlIdentifier, SqlNode> option : create.options()) {
      operandMap.put(option.left.getSimple(),
          requireNonNull(value(option.right)));
    }
    subSchema =
        schemaFactory.create(schemaInfo.schema().plus(), schemaInfo.name(), operandMap);
    schemaInfo.schema().add(schemaInfo.name(), subSchema);
  }

  /** Executes a {@code CREATE FUNCTION} command. */
  public void execute(SqlCreateFunction create,
      CalcitePrepare.Context context) {
    throw new UnsupportedOperationException("CREATE FUNCTION is not supported");
  }

  /**
   * Executes {@code DROP FUNCTION}, {@code DROP TABLE}, {@code DROP MATERIALIZED VIEW},
   * {@code DROP TYPE}, {@code DROP VIEW} commands.
   */
  public void execute(SqlDropObject drop,
      CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo =
        schema(context, false, drop.name);
    final @Nullable CalciteSchema schema =
        schemaInfo.schema(); // null if schema does not exist
    final String objectName = schemaInfo.name();

    boolean existed;
    switch (drop.getKind()) {
      case DROP_TABLE:
      case DROP_MATERIALIZED_VIEW:
        Table materializedView =
            schema != null
                && drop.getKind() == SqlKind.DROP_MATERIALIZED_VIEW
                    ? schema.plus().getTable(objectName)
                    : null;

        existed = schema != null && schema.removeTable(objectName);
        if (existed) {
          if (materializedView instanceof Wrapper) {
            ((Wrapper) materializedView).maybeUnwrap(MaterializationKey.class)
                .ifPresent(materializationKey -> MaterializationService.instance()
                    .removeMaterialization(materializationKey));
          }

          if (drop.getKind() == SqlKind.DROP_TABLE) {
            // For PostgreSQL, we also need to drop the physical table
            try {
              DataSource ds = getDataSource(context);
              try (Connection connection = ds.getConnection();
                  PreparedStatement stmt =
                      connection.prepareStatement("DROP TABLE IF EXISTS \"" + objectName + "\"")) {
                stmt.executeUpdate();
              }
            } catch (SQLException e) {
              throw new RuntimeException("Error dropping table in PostgreSQL: " + objectName, e);
            }
          } else if (drop.getKind() == SqlKind.DROP_MATERIALIZED_VIEW) {
            // For PostgreSQL, we also need to drop the physical materialized view
            try {
              DataSource ds = getDataSource(context);
              try (Connection connection = ds.getConnection();
                  PreparedStatement stmt = connection.prepareStatement(
                      "DROP MATERIALIZED VIEW IF EXISTS \"" + objectName + "\"")) {
                stmt.executeUpdate();
              }
            } catch (SQLException e) {
              throw new RuntimeException(
                  "Error dropping materialized view in PostgreSQL: " + objectName, e);
            }
          }
        } else if (!drop.ifExists) {
          throw SqlUtil.newContextException(drop.name.getParserPosition(),
              RESOURCE.tableNotFound(objectName));
        }
        break;
      case DROP_VIEW:
        // Not quite right: removes any other functions with the same name
        existed = schema != null && schema.removeFunction(objectName);
        if (existed) {
          // For PostgreSQL, we also need to drop the physical view
          try {
            DataSource ds = getDataSource(context);
            try (Connection connection = ds.getConnection();
                PreparedStatement stmt =
                    connection.prepareStatement("DROP VIEW IF EXISTS \"" + objectName + "\"")) {
              stmt.executeUpdate();
            }
          } catch (SQLException e) {
            throw new RuntimeException("Error dropping view in PostgreSQL: " + objectName, e);
          }
        } else if (!drop.ifExists) {
          throw SqlUtil.newContextException(drop.name.getParserPosition(),
              RESOURCE.viewNotFound(objectName));
        }
        break;
      case DROP_TYPE:
        existed = schema != null && schema.removeType(objectName);
        if (!existed && !drop.ifExists) {
          throw SqlUtil.newContextException(drop.name.getParserPosition(),
              RESOURCE.typeNotFound(objectName));
        }
        break;
      case DROP_FUNCTION:
        existed = schema != null && schema.removeFunction(objectName);
        if (!existed && !drop.ifExists) {
          throw SqlUtil.newContextException(drop.name.getParserPosition(),
              RESOURCE.functionNotFound(objectName));
        }
        break;
      case OTHER_DDL:
      default:
        throw new AssertionError(drop.getKind());
    }
  }

  /**
   * Executes a {@code TRUNCATE TABLE} command.
   */
  public void execute(SqlTruncateTable truncate,
      CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo =
        schema(context, true, truncate.name);
    if (schemaInfo.schema() == null
        || schemaInfo.schema().plus().getTable(schemaInfo.name()) == null) {
      throw SqlUtil.newContextException(truncate.name.getParserPosition(),
          RESOURCE.tableNotFound(schemaInfo.name()));
    }

    if (!truncate.continueIdentify) {
      // Calcite not support RESTART IDENTIFY
      throw new UnsupportedOperationException("RESTART IDENTIFY is not supported");
    }

    truncate(truncate.name, context, getDataSource(context));
  }

  /**
   * Creates a materialized view in PostgreSQL.
   */
  public void execute(SqlCreateMaterializedView create,
      CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo =
        schema(context, true, create.name);
    if (schemaInfo.schema() == null) {
      throw new RuntimeException("Schema " + create.name + " not found");
    }

    final String viewName = schemaInfo.name();
    final CalciteSchema schema = schemaInfo.schema();
    final SqlNode query = renameColumns(create.columnList, create.query);

    // Get target schema
    SqlValidator validator = validator(context, true);
    SqlNode validatedQuery = validator.validate(query);

    // Calculate view and underlying query SQL
    String viewSql = validatedQuery.toSqlString(CalciteSqlDialect.DEFAULT).getSql();
    String materializedViewSql = "CREATE MATERIALIZED VIEW \"" + viewName + "\" AS " + viewSql;

    // Create materialized view in PostgreSQL
    DataSource ds = getDataSource(context);
    try (Connection connection = ds.getConnection();
        PreparedStatement stmt = connection.prepareStatement(materializedViewSql)) {
      stmt.executeUpdate();

      // Wait a moment to ensure the materialized view is fully created
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      // Create the materialized view wrapper in Calcite
      try {
        // Create a wrapper for the PostgreSQL materialized view
        PostgresMaterializedView pgMaterializedView = new PostgresMaterializedView(ds, viewName);

        // Add the materialized view to the schema
        schema.add(viewName, pgMaterializedView);
      } catch (SQLException e) {
        throw new RuntimeException("Materialized view '" + viewName +
            "' was created in PostgreSQL, but could not create the wrapper: " + e.getMessage(), e);
      }
    } catch (SQLException e) {
      throw new RuntimeException(
          "Error creating materialized view '" + viewName + "': " + e.getMessage(), e);
    }
  }

  /** Executes a {@code CREATE SCHEMA} command. */
  public void execute(SqlCreateSchema create,
      CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo =
        schema(context, true, create.name);
    requireNonNull(schemaInfo.schema()); // TODO: should not assume parent schema exists
    if (schemaInfo.schema().plus().getSubSchema(schemaInfo.name()) != null) {
      if (create.ifNotExists) {
        return;
      }
      if (!create.getReplace()) {
        throw SqlUtil.newContextException(create.name.getParserPosition(),
            RESOURCE.schemaExists(schemaInfo.name()));
      }
    }

    // Create PostgreSQL schema
    DataSource ds = getDataSource(context);
    try {
      try (Connection connection = ds.getConnection();
          PreparedStatement stmt = connection.prepareStatement(
              "CREATE SCHEMA IF NOT EXISTS \"" + schemaInfo.name() + "\"")) {
        stmt.executeUpdate();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error creating schema in PostgreSQL: " + schemaInfo.name(), e);
    }

    final Schema subSchema = new AbstractSchema();
    schemaInfo.schema().add(schemaInfo.name(), subSchema);
  }

  /** Executes a {@code DROP SCHEMA} command. */
  public void execute(SqlDropSchema drop,
      CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo =
        schema(context, false, drop.name);
    final String name = schemaInfo.name();
    final boolean existed = schemaInfo.schema() != null
        && schemaInfo.schema().removeSubSchema(name);

    if (existed) {
      // Drop PostgreSQL schema
      DataSource ds = getDataSource(context);
      try {
        try (Connection connection = ds.getConnection();
            PreparedStatement stmt = connection.prepareStatement(
                "DROP SCHEMA IF EXISTS \"" + name + "\" CASCADE")) {
          stmt.executeUpdate();
        }
      } catch (SQLException e) {
        throw new RuntimeException("Error dropping schema in PostgreSQL: " + name, e);
      }
    } else if (!drop.ifExists) {
      throw SqlUtil.newContextException(drop.name.getParserPosition(),
          RESOURCE.schemaNotFound(name));
    }
  }

  /** Executes a {@code CREATE TABLE} command. */
  public void execute(SqlCreateTable create,
      CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo =
        schema(context, true, create.name);
    requireNonNull(schemaInfo.schema()); // TODO: should not assume parent schema exists
    final JavaTypeFactory typeFactory = context.getTypeFactory();
    final RelDataType queryRowType;
    if (create.query != null) {
      // A bit of a hack: pretend it's a view, to get its row type
      final String sql =
          create.query.toSqlString(CalciteSqlDialect.DEFAULT).getSql();
      final ViewTableMacro viewTableMacro =
          ViewTable.viewMacro(schemaInfo.schema().plus(), sql, schemaInfo.schema().path(null),
              context.getObjectPath(), false);
      final TranslatableTable x = viewTableMacro.apply(ImmutableList.of());
      queryRowType = x.getRowType(typeFactory);

      if (create.columnList != null
          && queryRowType.getFieldCount() != create.columnList.size()) {
        throw SqlUtil.newContextException(
            create.columnList.getParserPosition(),
            RESOURCE.columnCountMismatch());
      }
    } else {
      queryRowType = null;
    }
    final List<SqlNode> columnList;
    if (create.columnList != null) {
      columnList = create.columnList;
    } else {
      if (queryRowType == null) {
        // "CREATE TABLE t" is invalid; because there is no "AS query" we need
        // a list of column names and types, "CREATE TABLE t (INT c)".
        throw SqlUtil.newContextException(create.name.getParserPosition(),
            RESOURCE.createTableRequiresColumnList());
      }
      columnList = new ArrayList<>();
      for (String name : queryRowType.getFieldNames()) {
        columnList.add(new SqlIdentifier(name, SqlParserPos.ZERO));
      }
    }

    // Build the CREATE TABLE SQL statement for PostgreSQL
    StringBuilder createTableSql = new StringBuilder();
    createTableSql.append("CREATE TABLE ");
    if (create.ifNotExists) {
      createTableSql.append("IF NOT EXISTS ");
    }
    createTableSql.append("\"").append(schemaInfo.name()).append("\" (");

    boolean first = true;
    // Process column declarations for the CREATE TABLE statement
    final ImmutableList.Builder<ColumnDef> b = ImmutableList.builder();
    final RelDataTypeFactory.Builder storedBuilder = typeFactory.builder();
    final SqlValidator validator = validator(context, true);

    for (Ord<SqlNode> c : Ord.zip(columnList)) {
      if (!first) {
        createTableSql.append(", ");
      }
      first = false;

      if (c.e instanceof SqlColumnDeclaration) {
        final SqlColumnDeclaration d = (SqlColumnDeclaration) c.e;
        final RelDataType type = d.dataType.deriveType(validator, true);
        if (d.strategy != ColumnStrategy.VIRTUAL) {
          storedBuilder.add(d.name.getSimple(), type);
        }
        b.add(ColumnDef.of(d.expression, type, d.strategy));

        // Add column to SQL statement
        createTableSql.append("\"").append(d.name.getSimple()).append("\" ");
        createTableSql.append(PostgresTypeConversion.toPostgresTypeString(type));
        if (d.strategy == ColumnStrategy.NOT_NULLABLE) {
          createTableSql.append(" NOT NULL");
        }
      } else if (c.e instanceof SqlIdentifier) {
        final SqlIdentifier id = (SqlIdentifier) c.e;
        if (queryRowType == null) {
          throw SqlUtil.newContextException(id.getParserPosition(),
              RESOURCE.createTableRequiresColumnTypes(id.getSimple()));
        }
        final RelDataTypeField f = queryRowType.getFieldList().get(c.i);
        final ColumnStrategy strategy = f.getType().isNullable()
            ? ColumnStrategy.NULLABLE
            : ColumnStrategy.NOT_NULLABLE;
        b.add(ColumnDef.of(c.e, f.getType(), strategy));
        storedBuilder.add(id.getSimple(), f.getType());

        // Add column to SQL statement
        createTableSql.append("\"").append(id.getSimple()).append("\" ");
        createTableSql.append(PostgresTypeConversion.toPostgresTypeString(f.getType()));
        if (strategy == ColumnStrategy.NOT_NULLABLE) {
          createTableSql.append(" NOT NULL");
        }
      } else {
        throw new AssertionError(c.e.getClass());
      }
    }

    createTableSql.append(")");

    if (schemaInfo.schema().plus().getTable(schemaInfo.name()) != null) {
      // Table exists.
      if (create.ifNotExists) {
        return;
      }
      if (!create.getReplace()) {
        // They did not specify IF NOT EXISTS, so give error.
        throw SqlUtil.newContextException(create.name.getParserPosition(),
            RESOURCE.tableExists(schemaInfo.name()));
      }

      // Drop existing table
      try {
        DataSource ds = getDataSource(context);
        try (Connection connection = ds.getConnection();
            PreparedStatement stmt =
                connection.prepareStatement("DROP TABLE \"" + schemaInfo.name() + "\"")) {
          stmt.executeUpdate();
        }
      } catch (SQLException e) {
        throw new RuntimeException(
            "Error dropping existing table in PostgreSQL: " + schemaInfo.name(), e);
      }
    }

    // Create the table in PostgreSQL
    try {
      DataSource ds = getDataSource(context);
      try (Connection connection = ds.getConnection();
          PreparedStatement stmt = connection.prepareStatement(createTableSql.toString())) {
        stmt.executeUpdate();
      }

      // Create Calcite wrapper for the table
      PostgresModifiableTable table =
          new PostgresModifiableTable(ds, schemaInfo.name(), context.getTypeFactory());
      schemaInfo.schema().add(schemaInfo.name(), table);

      // Populate the table if query is provided
      if (create.query != null) {
        populate(create.name, create.query, context);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error creating table in PostgreSQL: " + schemaInfo.name(), e);
    }
  }

  /** Executes a {@code CREATE VIEW} command. */
  public void execute(SqlCreateView create,
      CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo =
        schema(context, true, create.name);
    requireNonNull(schemaInfo.schema()); // TODO: should not assume parent schema exists
    final SchemaPlus schemaPlus = schemaInfo.schema().plus();
    for (Function function : schemaPlus.getFunctions(schemaInfo.name())) {
      if (function.getParameters().isEmpty()) {
        if (!create.getReplace()) {
          throw SqlUtil.newContextException(create.name.getParserPosition(),
              RESOURCE.viewExists(schemaInfo.name()));
        }
        schemaInfo.schema().removeFunction(schemaInfo.name());
      }
    }

    final SqlNode q = renameColumns(create.columnList, create.query);
    final String sql = q.toSqlString(CalciteSqlDialect.DEFAULT).getSql();

    // Create the view in PostgreSQL
    try {
      DataSource ds = getDataSource(context);
      String createViewSql = "CREATE";
      if (create.getReplace()) {
        createViewSql += " OR REPLACE";
      }
      createViewSql += " VIEW \"" + schemaInfo.name() + "\" AS " + sql;

      try (Connection connection = ds.getConnection();
          PreparedStatement stmt = connection.prepareStatement(createViewSql)) {
        stmt.executeUpdate();
      }

      // Create Calcite wrapper for the view
      final ViewTableMacro viewTableMacro =
          ViewTable.viewMacro(schemaPlus, sql, schemaInfo.schema().path(null),
              context.getObjectPath(), false);
      schemaPlus.add(schemaInfo.name(), viewTableMacro);
    } catch (SQLException e) {
      throw new RuntimeException("Error creating view in PostgreSQL: " + schemaInfo.name(), e);
    }
  }

  /** Column definition. */
  private static class ColumnDef {
    final @Nullable SqlNode expr;
    final RelDataType type;
    final ColumnStrategy strategy;

    private ColumnDef(@Nullable SqlNode expr, RelDataType type,
        ColumnStrategy strategy) {
      this.expr = expr;
      this.type = type;
      this.strategy = requireNonNull(strategy, "strategy");
      checkArgument(
          strategy == ColumnStrategy.NULLABLE
              || strategy == ColumnStrategy.NOT_NULLABLE
              || expr != null);
    }

    static ColumnDef of(@Nullable SqlNode expr, RelDataType type,
        ColumnStrategy strategy) {
      return new ColumnDef(expr, type, strategy);
    }
  }
}
