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

package com.baremaps.postgres.metadata;



import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;

public class DatabaseMetadata {

  private static final String TABLE_CAT = "TABLE_CAT";
  private static final String TABLE_SCHEM = "TABLE_SCHEM";
  private static final String TABLE_NAME = "TABLE_NAME";
  private static final String TABLE_TYPE = "TABLE_TYPE";
  private static final String REMARKS = "REMARKS";
  private static final String TYPE_CAT = "TYPE_CAT";
  private static final String TYPE_SCHEM = "TYPE_SCHEM";
  private static final String TYPE_NAME = "TYPE_NAME";
  private static final String SELF_REFERENCING_COL_NAME = "SELF_REFERENCING_COL_NAME";
  private static final String REF_GENERATION = "REF_GENERATION";
  private static final String COLUMN_NAME = "COLUMN_NAME";
  private static final String DATA_TYPE = "DATA_TYPE";
  private static final String COLUMN_SIZE = "COLUMN_SIZE";
  private static final String DECIMAL_DIGITS = "DECIMAL_DIGITS";
  private static final String NUM_PREC_RADIX = "NUM_PREC_RADIX";
  private static final String NULLABLE = "NULLABLE";
  private static final String COLUMN_DEF = "COLUMN_DEF";
  private static final String KEY_SEQ = "KEY_SEQ";
  private static final String PK_NAME = "PK_NAME";
  private static final String SQL_DATA_TYPE = "SQL_DATA_TYPE";
  private static final String SQL_DATETIME_SUB = "SQL_DATETIME_SUB";
  private static final String CHAR_OCTET_LENGTH = "CHAR_OCTET_LENGTH";
  private static final String ORDINAL_POSITION = "ORDINAL_POSITION";
  private static final String IS_NULLABLE = "IS_NULLABLE";
  private static final String SCOPE_CATALOG = "SCOPE_CATALOG";
  private static final String SCOPE_SCHEMA = "SCOPE_SCHEMA";
  private static final String SCOPE_TABLE = "SCOPE_TABLE";
  private static final String SOURCE_DATA_TYPE = "SOURCE_DATA_TYPE";
  private static final String IS_AUTOINCREMENT = "IS_AUTOINCREMENT";
  private static final String IS_GENERATEDCOLUMN = "IS_GENERATEDCOLUMN";

  private final DataSource dataSource;

  public DatabaseMetadata(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public List<TableMetadata> getTableMetaData() throws SQLException {
    return getTableMetaData(null, null, null, null);
  }

  public List<TableMetadata> getTableMetaData(
      String catalog,
      String schema,
      String tableNamePattern,
      String[] types) throws SQLException {
    Map<String, TableResult> descriptions = getTables(catalog, schema, tableNamePattern, types)
        .stream().collect(Collectors.toMap(TableResult::tableName, Function.identity()));
    Map<String, List<ColumnResult>> columns = getColumns(catalog, schema, tableNamePattern, null)
        .stream().collect(Collectors.groupingBy(ColumnResult::tableName));
    Map<String, List<PrimaryKeyResult>> primaryKeys =
        getPrimaryKeys(catalog, schema, tableNamePattern).stream()
            .collect(Collectors.groupingBy(PrimaryKeyResult::tableName));
    return descriptions.entrySet().stream()
        .map(entry -> new TableMetadata(entry.getValue(),
            primaryKeys.getOrDefault(entry.getKey(), List.of()),
            columns.getOrDefault(entry.getKey(), List.of())))
        .toList();
  }

  private List<TableResult> getTables(
      String catalog,
      String schemaPattern,
      String tableNamePattern,
      String[] types) throws SQLException {
    var tableDescriptions = new ArrayList<TableResult>();
    try (var connection = dataSource.getConnection();
        var resultSet =
            connection.getMetaData().getTables(catalog, schemaPattern, tableNamePattern, types)) {
      while (resultSet.next()) {
        tableDescriptions.add(new TableResult(
            resultSet.getString(TABLE_CAT),
            resultSet.getString(TABLE_SCHEM),
            resultSet.getString(TABLE_NAME),
            resultSet.getString(TABLE_TYPE),
            resultSet.getString(REMARKS),
            resultSet.getString(TYPE_CAT),
            resultSet.getString(TYPE_SCHEM),
            resultSet.getString(TYPE_NAME),
            resultSet.getString(SELF_REFERENCING_COL_NAME),
            resultSet.getString(REF_GENERATION)));
      }
    }
    return tableDescriptions;
  }

  private List<ColumnResult> getColumns(
      String catalog,
      String schemaPattern,
      String tableNamePattern,
      String columnNamePattern) throws SQLException {
    var tableColumns = new ArrayList<ColumnResult>();
    try (var connection = dataSource.getConnection();
        var resultSet = connection.getMetaData().getColumns(catalog, schemaPattern,
            tableNamePattern, columnNamePattern)) {
      while (resultSet.next()) {
        tableColumns.add(new ColumnResult(
            resultSet.getString(TABLE_CAT),
            resultSet.getString(TABLE_SCHEM),
            resultSet.getString(TABLE_NAME),
            resultSet.getString(COLUMN_NAME),
            resultSet.getInt(DATA_TYPE),
            resultSet.getString(TYPE_NAME),
            resultSet.getInt(COLUMN_SIZE),
            resultSet.getInt(DECIMAL_DIGITS),
            resultSet.getInt(NUM_PREC_RADIX),
            resultSet.getInt(NULLABLE),
            resultSet.getString(REMARKS),
            resultSet.getString(COLUMN_DEF),
            resultSet.getInt(SQL_DATA_TYPE),
            resultSet.getInt(SQL_DATETIME_SUB),
            resultSet.getInt(CHAR_OCTET_LENGTH),
            resultSet.getInt(ORDINAL_POSITION),
            resultSet.getString(IS_NULLABLE),
            resultSet.getString(SCOPE_CATALOG),
            resultSet.getString(SCOPE_SCHEMA),
            resultSet.getString(SCOPE_TABLE),
            resultSet.getShort(SOURCE_DATA_TYPE),
            resultSet.getString(IS_AUTOINCREMENT),
            resultSet.getString(IS_GENERATEDCOLUMN)));
      }
    }
    return tableColumns;
  }

  private List<PrimaryKeyResult> getPrimaryKeys(
      String catalog,
      String schemaPattern,
      String table) throws SQLException {
    var tablePrimaryKeyColumns = new ArrayList<PrimaryKeyResult>();
    try (var connection = dataSource.getConnection();
        var resultSet = connection.getMetaData().getPrimaryKeys(catalog, schemaPattern, table)) {
      while (resultSet.next()) {
        tablePrimaryKeyColumns.add(new PrimaryKeyResult(
            resultSet.getString(TABLE_CAT),
            resultSet.getString(TABLE_SCHEM),
            resultSet.getString(TABLE_NAME),
            resultSet.getString(COLUMN_NAME),
            resultSet.getShort(KEY_SEQ),
            resultSet.getString(PK_NAME)));
      }
    }
    return tablePrimaryKeyColumns;
  }
}
