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

package com.baremaps.calcite.ddl;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.util.ImmutableNullableList;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Parse tree for {@code CREATE TYPE} statement.
 */
public class SqlCreateType extends SqlCreate {
  public final SqlIdentifier name;
  public final @Nullable SqlNodeList attributeDefs;
  public final @Nullable SqlDataTypeSpec dataType;

  private static final SqlOperator OPERATOR =
      new SqlSpecialOperator("CREATE TYPE", SqlKind.CREATE_TYPE);

  /** Creates a SqlCreateType. */
  SqlCreateType(SqlParserPos pos, boolean replace, SqlIdentifier name,
      @Nullable SqlNodeList attributeDefs, @Nullable SqlDataTypeSpec dataType) {
    super(OPERATOR, pos, replace, false);
    this.name = requireNonNull(name, "name");
    this.attributeDefs = attributeDefs; // may be null
    this.dataType = dataType; // may be null
  }

  @SuppressWarnings("nullness")
  @Override
  public List<SqlNode> getOperandList() {
    return ImmutableNullableList.of(name, attributeDefs);
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    if (getReplace()) {
      writer.keyword("CREATE OR REPLACE");
    } else {
      writer.keyword("CREATE");
    }
    writer.keyword("TYPE");
    name.unparse(writer, leftPrec, rightPrec);
    writer.keyword("AS");
    if (attributeDefs != null) {
      SqlWriter.Frame frame = writer.startList("(", ")");
      for (SqlNode a : attributeDefs) {
        writer.sep(",");
        a.unparse(writer, 0, 0);
      }
      writer.endList(frame);
    } else if (dataType != null) {
      dataType.unparse(writer, leftPrec, rightPrec);
    }
  }
}
