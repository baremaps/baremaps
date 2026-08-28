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

/**
 * Parse tree for {@code CREATE SCHEMA} statement.
 */
public class SqlCreateSchema extends SqlCreate {
  public final SqlIdentifier name;

  private static final SqlOperator OPERATOR =
      new SqlSpecialOperator("CREATE SCHEMA", SqlKind.CREATE_SCHEMA);

  /** Creates a SqlCreateSchema. */
  SqlCreateSchema(SqlParserPos pos, boolean replace, boolean ifNotExists,
      SqlIdentifier name) {
    super(OPERATOR, pos, replace, ifNotExists);
    this.name = requireNonNull(name, "name");
  }

  @Override
  public List<SqlNode> getOperandList() {
    return ImmutableNullableList.of(name);
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    if (getReplace()) {
      writer.keyword("CREATE OR REPLACE");
    } else {
      writer.keyword("CREATE");
    }
    writer.keyword("SCHEMA");
    if (ifNotExists) {
      writer.keyword("IF NOT EXISTS");
    }
    name.unparse(writer, leftPrec, rightPrec);
  }
}
