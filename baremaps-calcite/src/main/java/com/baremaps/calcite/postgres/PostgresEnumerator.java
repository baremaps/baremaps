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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.sql.type.SqlTypeName;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;

/**
 * Streams the rows of a PostgreSQL table. Geometry columns are selected as WKB and decoded into JTS
 * geometries.
 */
final class PostgresEnumerator implements Enumerator<Object[]> {

  private final DataSource dataSource;
  private final String query;
  private final List<RelDataTypeField> fields;
  private Connection connection;
  private Statement statement;
  private ResultSet resultSet;
  private Object[] current;

  PostgresEnumerator(DataSource dataSource, String qualifiedName, RelDataType rowType) {
    this.dataSource = dataSource;
    this.fields = rowType.getFieldList();
    this.query = "SELECT " + fields.stream().map(PostgresEnumerator::projection)
        .collect(Collectors.joining(", ")) + " FROM " + qualifiedName;
    open();
  }

  private static String projection(RelDataTypeField field) {
    String column = "\"" + field.getName() + "\"";
    return field.getType().getSqlTypeName() == SqlTypeName.GEOMETRY
        ? "ST_AsBinary(" + column + ") AS " + column
        : column;
  }

  private void open() {
    try {
      connection = dataSource.getConnection();
      statement = connection.createStatement();
      resultSet = statement.executeQuery(query);
    } catch (SQLException e) {
      close();
      throw new IllegalStateException("Failed to execute: " + query, e);
    }
  }

  @Override
  public Object[] current() {
    return current;
  }

  @Override
  public boolean moveNext() {
    try {
      if (!resultSet.next()) {
        current = null;
        return false;
      }
      current = new Object[fields.size()];
      for (int i = 0; i < fields.size(); i++) {
        current[i] = fields.get(i).getType().getSqlTypeName() == SqlTypeName.GEOMETRY
            ? readGeometry(resultSet.getBytes(i + 1))
            : resultSet.getObject(i + 1);
      }
      return true;
    } catch (SQLException e) {
      close();
      throw new IllegalStateException("Failed to read the next row", e);
    }
  }

  private static Geometry readGeometry(byte[] wkb) {
    if (wkb == null) {
      return null;
    }
    try {
      return new WKBReader().read(wkb);
    } catch (ParseException e) {
      throw new IllegalStateException("Invalid WKB geometry", e);
    }
  }

  @Override
  public void reset() {
    close();
    open();
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
      throw new IllegalStateException("Failed to close the connection", e);
    }
  }
}
