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

package com.baremaps.calcite;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.apache.calcite.util.Static.RESOURCE;

import com.baremaps.calcite.data.DataDdlBackend;
import com.baremaps.calcite.ddl.SqlAttributeDefinition;
import com.baremaps.calcite.ddl.SqlColumnDeclaration;
import com.baremaps.calcite.ddl.SqlCreateForeignSchema;
import com.baremaps.calcite.ddl.SqlCreateFunction;
import com.baremaps.calcite.ddl.SqlCreateMaterializedView;
import com.baremaps.calcite.ddl.SqlCreateSchema;
import com.baremaps.calcite.ddl.SqlCreateTable;
import com.baremaps.calcite.ddl.SqlCreateTableLike;
import com.baremaps.calcite.ddl.SqlCreateType;
import com.baremaps.calcite.ddl.SqlCreateView;
import com.baremaps.calcite.ddl.SqlDropObject;
import com.baremaps.calcite.ddl.SqlDropSchema;
import com.baremaps.calcite.ddl.SqlTruncateTable;
import com.baremaps.calcite.sql.BaremapsSqlDdlParser;
import com.google.common.collect.ImmutableList;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.apache.calcite.schema.Function;
import org.apache.calcite.schema.ModifiableTable;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.calcite.schema.Wrapper;
import org.apache.calcite.schema.impl.ViewTable;
import org.apache.calcite.schema.impl.ViewTableMacro;
import org.apache.calcite.server.DdlExecutor;
import org.apache.calcite.server.DdlExecutorImpl;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlDataTypeSpec;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlUtil;
import org.apache.calcite.sql.SqlWriterConfig;
import org.apache.calcite.sql.dialect.CalciteSqlDialect;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlAbstractParserImpl;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParserImplFactory;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.pretty.SqlPrettyWriter;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.apache.calcite.util.NlsString;
import org.apache.calcite.util.Pair;
import org.apache.calcite.util.Util;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Executes the DDL statements of the Baremaps SQL dialect.
 *
 * <p>
 * This is Calcite's {@code ServerDdlExecutor} with the storage-specific parts moved behind a
 * {@link DdlBackend}: everything expressible in terms of Calcite alone lives here, so that adding a
 * storage only requires a backend. {@link DdlExecutorImpl} dispatches each statement to the
 * {@code execute} overload matching its {@link SqlNode} subclass.
 *
 * <p>
 * Calcite looks the executor up by the name of a static field, e.g.
 * {@code parserFactory=com.baremaps.calcite.BaremapsDdlExecutor#PARSER_FACTORY}, which is why the
 * backends are exposed as constants rather than injected.
 */
public class BaremapsDdlExecutor extends DdlExecutorImpl {

  /** Parser factory whose DDL is backed by the {@code data} package (memory-mapped tables). */
  @SuppressWarnings("unused") // used via reflection
  public static final SqlParserImplFactory PARSER_FACTORY =
      parserFactory(new BaremapsDdlExecutor(new DataDdlBackend()));

  private final DdlBackend backend;

  protected BaremapsDdlExecutor(DdlBackend backend) {
    this.backend = requireNonNull(backend, "backend");
  }

  /**
   * Returns a parser factory for the Baremaps SQL dialect that executes DDL with the given
   * executor.
   */
  protected static SqlParserImplFactory parserFactory(DdlExecutor executor) {
    return new SqlParserImplFactory() {
      @Override
      public SqlAbstractParserImpl getParser(Reader stream) {
        return BaremapsSqlDdlParser.FACTORY.getParser(stream);
      }

      @Override
      public DdlExecutor getDdlExecutor() {
        return executor;
      }
    };
  }

  /**
   * An object name split into the schema that will hold it (null if absent) and its simple name.
   */
  private record SchemaInfo(String name, @Nullable CalciteSchema schema) {

    CalciteSchema requireSchema() {
      // TODO: should not assume the parent schema exists
      return requireNonNull(schema, "schema");
    }

    SchemaPlus plus() {
      return requireSchema().plus();
    }

    boolean tableExists() {
      return schema != null && schema.plus().getTable(name) != null;
    }

    boolean viewExists() {
      if (schema == null) {
        return false;
      }
      for (Function function : schema.plus().getFunctions(name)) {
        if (function.getParameters().isEmpty()) {
          return true;
        }
      }
      return false;
    }
  }

  private static SchemaInfo schema(CalcitePrepare.Context context, boolean mutable,
      SqlIdentifier id) {
    final String name;
    final List<String> path;
    if (id.isSimple()) {
      path = context.getDefaultSchemaPath();
      name = id.getSimple();
    } else {
      path = Util.skipLast(id.names);
      name = Util.last(id.names);
    }
    CalciteSchema schema = mutable ? context.getMutableRootSchema() : context.getRootSchema();
    for (String p : path) {
      CalciteSchema subSchema = schema.getSubSchema(p, true);
      if (subSchema == null) {
        return new SchemaInfo(name, null);
      }
      schema = subSchema;
    }
    return new SchemaInfo(name, schema);
  }

  private static SqlValidator validator(CalcitePrepare.Context context, boolean mutable) {
    return new ContextSqlValidator(context, mutable);
  }

  /** Wraps a query to rename its columns. Used by CREATE VIEW and CREATE MATERIALIZED VIEW. */
  private static SqlNode renameColumns(@Nullable SqlNodeList columnList, SqlNode query) {
    if (columnList == null) {
      return query;
    }
    final SqlParserPos p = query.getParserPosition();
    final SqlCall from = SqlStdOperatorTable.AS.createCall(p,
        ImmutableList.<SqlNode>builder()
            .add(query)
            .add(new SqlIdentifier("_", p))
            .addAll(columnList)
            .build());
    return new SqlSelect(p, null, SqlNodeList.SINGLETON_STAR, from, null, null, null, null,
        null, null, null, null, null);
  }

  /**
   * Returns a view macro for {@code query} in {@code schema}; validates the query as a side effect.
   */
  private static ViewTableMacro viewMacro(CalcitePrepare.Context context, SchemaInfo schemaInfo,
      SqlNode query) {
    final String sql = query.toSqlString(CalciteSqlDialect.DEFAULT).getSql();
    return ViewTable.viewMacro(schemaInfo.plus(), sql, schemaInfo.requireSchema().path(null),
        context.getObjectPath(), false);
  }

  /**
   * Populates the table called {@code name} by executing {@code INSERT INTO name query}. The
   * statement is planned with the target schema as default so that a simple {@code name} resolves
   * to the table just created; the root schema stays on the path for the query's own tables.
   */
  private static void populate(SchemaInfo target, SqlIdentifier name, SqlNode query,
      CalcitePrepare.Context context) {
    // Converting from SqlNode to SQL and back is wasteful, but the planner only accepts text.
    final FrameworkConfig config = Frameworks.newConfigBuilder()
        .defaultSchema(target.plus())
        .build();
    final Planner planner = Frameworks.getPlanner(config);
    try {
      final StringBuilder buf = new StringBuilder();
      final SqlWriterConfig writerConfig = SqlPrettyWriter.config().withAlwaysUseParentheses(false);
      final SqlPrettyWriter w = new SqlPrettyWriter(writerConfig, buf);
      buf.append("INSERT INTO ");
      name.unparse(w, 0, 0);
      buf.append(' ');
      query.unparse(w, 0, 0);
      final SqlNode parsed = planner.parse(buf.toString());
      final SqlNode validated = planner.validate(parsed);
      final RelRoot root = planner.rel(validated);
      try (PreparedStatement prepare = context.getRelRunner().prepareStatement(root.rel)) {
        prepare.executeUpdate();
      }
    } catch (SqlParseException | ValidationException | RelConversionException
        | SQLException e) {
      throw Util.throwAsRuntime(e);
    }
  }

  /** Returns the value of a literal, converting {@link NlsString} into String. */
  @SuppressWarnings("rawtypes")
  private static @Nullable Comparable value(SqlNode node) {
    final Comparable v = SqlLiteral.value(node);
    return v instanceof NlsString ? ((NlsString) v).getValue() : v;
  }

  /** Converts a {@code WITH (key = 'value', ...)} list into a map with lower-case keys. */
  private static Map<String, String> options(@Nullable SqlNodeList withOptions) {
    Map<String, String> options = new LinkedHashMap<>();
    if (withOptions == null) {
      return options;
    }
    for (SqlNode option : withOptions) {
      List<SqlNode> operands = ((SqlBasicCall) option).getOperandList();
      String key = ((SqlIdentifier) operands.get(0)).getSimple().toLowerCase(Locale.ROOT);
      options.put(key, String.valueOf(value(operands.get(1))));
    }
    return options;
  }

  /** Executes a {@code CREATE FOREIGN SCHEMA} command. */
  public void execute(SqlCreateForeignSchema create, CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo = schema(context, true, create.name);
    if (schemaInfo.plus().getSubSchema(schemaInfo.name()) != null
        && !create.getReplace() && !create.ifNotExists) {
      throw SqlUtil.newContextException(create.name.getParserPosition(),
          RESOURCE.schemaExists(schemaInfo.name()));
    }
    final String libraryName;
    if (create.type != null) {
      checkArgument(create.library == null);
      final String typeName = (String) requireNonNull(value(create.type));
      final JsonSchema.Type type =
          Util.enumVal(JsonSchema.Type.class, typeName.toUpperCase(Locale.ROOT));
      if (type != JsonSchema.Type.JDBC) {
        throw SqlUtil.newContextException(create.type.getParserPosition(),
            RESOURCE.schemaInvalidType(typeName, Arrays.toString(JsonSchema.Type.values())));
      }
      libraryName = JdbcSchema.Factory.class.getName();
    } else {
      libraryName = requireNonNull((String) value(requireNonNull(create.library)));
    }
    final SchemaFactory schemaFactory =
        AvaticaUtils.instantiatePlugin(SchemaFactory.class, libraryName);
    final Map<String, Object> operandMap = new LinkedHashMap<>();
    for (Pair<SqlIdentifier, SqlNode> option : create.options()) {
      operandMap.put(option.left.getSimple(), requireNonNull(value(option.right)));
    }
    final Schema subSchema =
        schemaFactory.create(schemaInfo.plus(), schemaInfo.name(), operandMap);
    schemaInfo.requireSchema().add(schemaInfo.name(), subSchema);
  }

  /** Executes a {@code CREATE FUNCTION} command. */
  public void execute(SqlCreateFunction create, CalcitePrepare.Context context) {
    throw new UnsupportedOperationException("CREATE FUNCTION is not supported");
  }

  /**
   * Executes {@code DROP FUNCTION}, {@code DROP TABLE}, {@code DROP MATERIALIZED VIEW},
   * {@code DROP TYPE}, {@code DROP VIEW} commands.
   */
  public void execute(SqlDropObject drop, CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo = schema(context, false, drop.name);
    final CalciteSchema schema = schemaInfo.schema();
    final String name = schemaInfo.name();
    final boolean existed;
    switch (drop.getKind()) {
      case DROP_TABLE:
      case DROP_MATERIALIZED_VIEW:
        existed = schemaInfo.tableExists();
        if (existed) {
          Table table = schemaInfo.plus().getTable(name);
          if (table instanceof Wrapper wrapper) {
            wrapper.maybeUnwrap(MaterializationKey.class)
                .ifPresent(key -> MaterializationService.instance().removeMaterialization(key));
          }
          backend.dropTable(schema, name);
        } else if (!drop.ifExists) {
          throw SqlUtil.newContextException(drop.name.getParserPosition(),
              RESOURCE.tableNotFound(name));
        }
        break;
      case DROP_VIEW:
        existed = schemaInfo.viewExists();
        if (existed) {
          backend.dropView(schema, name);
        } else if (!drop.ifExists) {
          throw SqlUtil.newContextException(drop.name.getParserPosition(),
              RESOURCE.viewNotFound(name));
        }
        break;
      case DROP_TYPE:
        existed = schema != null && schema.removeType(name);
        if (!existed && !drop.ifExists) {
          throw SqlUtil.newContextException(drop.name.getParserPosition(),
              RESOURCE.typeNotFound(name));
        }
        break;
      case DROP_FUNCTION:
        existed = schema != null && schema.removeFunction(name);
        if (!existed && !drop.ifExists) {
          throw SqlUtil.newContextException(drop.name.getParserPosition(),
              RESOURCE.functionNotFound(name));
        }
        break;
      default:
        throw new AssertionError(drop.getKind());
    }
  }

  /** Executes a {@code TRUNCATE TABLE} command. */
  public void execute(SqlTruncateTable truncate, CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo = schema(context, true, truncate.name);
    if (!schemaInfo.tableExists()) {
      throw SqlUtil.newContextException(truncate.name.getParserPosition(),
          RESOURCE.tableNotFound(schemaInfo.name()));
    }
    if (!truncate.continueIdentify) {
      throw new UnsupportedOperationException("RESTART IDENTITY is not supported");
    }
    Table table = schemaInfo.plus().getTable(schemaInfo.name());
    if (!(table instanceof ModifiableTable modifiable)) {
      throw new UnsupportedOperationException(
          "Table " + schemaInfo.name() + " is read-only and cannot be truncated");
    }
    modifiable.getModifiableCollection().clear();
  }

  /** Executes a {@code CREATE MATERIALIZED VIEW} command. */
  public void execute(SqlCreateMaterializedView create, CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo = schema(context, true, create.name);
    if (schemaInfo.tableExists()) {
      if (create.ifNotExists) {
        return;
      }
      throw SqlUtil.newContextException(create.name.getParserPosition(),
          RESOURCE.tableExists(schemaInfo.name()));
    }
    final SqlNode query = renameColumns(create.columnList, create.query);
    final String sql = query.toSqlString(CalciteSqlDialect.DEFAULT).getSql();
    final TranslatableTable view = viewMacro(context, schemaInfo, query).apply(ImmutableList.of());
    final RelDataType rowType = view.getRowType(context.getTypeFactory());
    backend.createMaterializedView(schemaInfo.requireSchema(), schemaInfo.name(), rowType, sql,
        context.getTypeFactory(), () -> populate(schemaInfo, create.name, create.query, context));
  }

  /** Executes a {@code CREATE SCHEMA} command. */
  public void execute(SqlCreateSchema create, CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo = schema(context, true, create.name);
    if (schemaInfo.plus().getSubSchema(schemaInfo.name()) != null) {
      if (create.ifNotExists) {
        return;
      }
      if (!create.getReplace()) {
        throw SqlUtil.newContextException(create.name.getParserPosition(),
            RESOURCE.schemaExists(schemaInfo.name()));
      }
    }
    backend.createSchema(schemaInfo.requireSchema(), schemaInfo.name());
  }

  /** Executes a {@code DROP SCHEMA} command. */
  public void execute(SqlDropSchema drop, CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo = schema(context, false, drop.name);
    final boolean exists = schemaInfo.schema() != null
        && schemaInfo.plus().getSubSchema(schemaInfo.name()) != null;
    if (exists) {
      backend.dropSchema(schemaInfo.requireSchema(), schemaInfo.name());
    } else if (!drop.ifExists) {
      throw SqlUtil.newContextException(drop.name.getParserPosition(),
          RESOURCE.schemaNotFound(schemaInfo.name()));
    }
  }

  /** Executes a {@code CREATE TABLE} command. */
  public void execute(SqlCreateTable create, CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo = schema(context, true, create.name);
    final RelDataType rowType = rowType(create, schemaInfo, context);
    if (schemaInfo.tableExists()) {
      if (create.ifNotExists) {
        return;
      }
      if (!create.getReplace()) {
        throw SqlUtil.newContextException(create.name.getParserPosition(),
            RESOURCE.tableExists(schemaInfo.name()));
      }
      backend.dropTable(schemaInfo.requireSchema(), schemaInfo.name());
    }
    Runnable populate = create.query == null
        ? () -> {
        }
        : () -> populate(schemaInfo, create.name, create.query, context);
    backend.createTable(schemaInfo.requireSchema(), schemaInfo.name(), rowType,
        options(create.withOptions), context.getTypeFactory(), populate);
  }

  /** Derives the row type of a {@code CREATE TABLE} from its column list and/or its query. */
  private static RelDataType rowType(SqlCreateTable create, SchemaInfo schemaInfo,
      CalcitePrepare.Context context) {
    final RelDataTypeFactory typeFactory = context.getTypeFactory();
    final RelDataType queryRowType;
    if (create.query != null) {
      // A bit of a hack: pretend it's a view, to get its row type
      final TranslatableTable view =
          viewMacro(context, schemaInfo, create.query).apply(ImmutableList.of());
      queryRowType = view.getRowType(typeFactory);
      if (create.columnList != null
          && queryRowType.getFieldCount() != create.columnList.size()) {
        throw SqlUtil.newContextException(create.columnList.getParserPosition(),
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
    final RelDataTypeFactory.Builder builder = typeFactory.builder();
    final SqlValidator validator = validator(context, true);
    for (Ord<SqlNode> c : Ord.zip(columnList)) {
      if (c.e instanceof SqlColumnDeclaration d) {
        builder.add(d.name.getSimple(), d.dataType.deriveType(validator, true));
      } else if (c.e instanceof SqlIdentifier id) {
        if (queryRowType == null) {
          throw SqlUtil.newContextException(id.getParserPosition(),
              RESOURCE.createTableRequiresColumnTypes(id.getSimple()));
        }
        final RelDataTypeField f = queryRowType.getFieldList().get(c.i);
        builder.add(id.getSimple(), f.getType());
      } else {
        throw new AssertionError(c.e.getClass());
      }
    }
    return builder.build();
  }

  /** Executes a {@code CREATE TABLE LIKE} command. */
  public void execute(SqlCreateTableLike create, CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo = schema(context, true, create.name);
    if (schemaInfo.tableExists()) {
      if (create.ifNotExists) {
        return;
      }
      if (!create.getReplace()) {
        throw SqlUtil.newContextException(create.name.getParserPosition(),
            RESOURCE.tableExists(schemaInfo.name()));
      }
      backend.dropTable(schemaInfo.requireSchema(), schemaInfo.name());
    }
    final SchemaInfo sourceInfo = schema(context, true, create.sourceTable);
    final CalciteSchema.TableEntry tableEntry = sourceInfo.requireSchema()
        .getTable(sourceInfo.name(), context.config().caseSensitive());
    final Table source = requireNonNull(tableEntry, "tableEntry").getTable();
    final RelDataType rowType = source.getRowType(context.getTypeFactory());
    backend.createTable(schemaInfo.requireSchema(), schemaInfo.name(), rowType, Map.of(),
        context.getTypeFactory(), () -> {
        });
  }

  /** Executes a {@code CREATE TYPE} command. */
  public void execute(SqlCreateType create, CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo = schema(context, true, create.name);
    final SqlValidator validator = validator(context, false);
    schemaInfo.requireSchema().add(schemaInfo.name(), typeFactory -> {
      if (create.dataType != null) {
        return create.dataType.deriveType(validator);
      }
      final RelDataTypeFactory.Builder builder = typeFactory.builder();
      if (create.attributeDefs != null) {
        for (SqlNode def : create.attributeDefs) {
          final SqlAttributeDefinition attributeDef = (SqlAttributeDefinition) def;
          final SqlDataTypeSpec typeSpec = attributeDef.dataType;
          builder.add(attributeDef.name.getSimple(), typeSpec.deriveType(validator));
        }
      }
      return builder.build();
    });
  }

  /** Executes a {@code CREATE VIEW} command. */
  public void execute(SqlCreateView create, CalcitePrepare.Context context) {
    final SchemaInfo schemaInfo = schema(context, true, create.name);
    if (schemaInfo.viewExists()) {
      if (!create.getReplace()) {
        throw SqlUtil.newContextException(create.name.getParserPosition(),
            RESOURCE.viewExists(schemaInfo.name()));
      }
      backend.dropView(schemaInfo.requireSchema(), schemaInfo.name());
    }
    final SqlNode query = renameColumns(create.columnList, create.query);
    final String sql = query.toSqlString(CalciteSqlDialect.DEFAULT).getSql();
    final ViewTableMacro view = viewMacro(context, schemaInfo, query);
    Util.discard(view.apply(ImmutableList.of())); // validates the query
    backend.createView(schemaInfo.requireSchema(), schemaInfo.name(), sql, create.getReplace(),
        view);
  }
}
