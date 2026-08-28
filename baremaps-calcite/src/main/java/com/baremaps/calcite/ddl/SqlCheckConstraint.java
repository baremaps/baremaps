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

import java.util.List;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.util.ImmutableNullableList;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Parse tree for {@code UNIQUE}, {@code PRIMARY KEY} constraints.
 *
 * <p>
 * And {@code FOREIGN KEY}, when we support it.
 */
public class SqlCheckConstraint extends SqlCall {
  private static final SqlSpecialOperator OPERATOR =
      new SqlSpecialOperator("CHECK", SqlKind.CHECK);

  private final @Nullable SqlIdentifier name;
  private final SqlNode expression;

  /** Creates a SqlCheckConstraint; use {@link SqlDdlNodes#check}. */
  SqlCheckConstraint(SqlParserPos pos, @Nullable SqlIdentifier name,
      SqlNode expression) {
    super(pos);
    this.name = name; // may be null
    this.expression = expression;
  }

  @Override
  public SqlOperator getOperator() {
    return OPERATOR;
  }

  @SuppressWarnings("nullness")
  @Override
  public List<SqlNode> getOperandList() {
    return ImmutableNullableList.of(name, expression);
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    if (name != null) {
      writer.keyword("CONSTRAINT");
      name.unparse(writer, 0, 0);
    }
    writer.keyword("CHECK");
    if (writer.isAlwaysUseParentheses()) {
      expression.unparse(writer, 0, 0);
    } else {
      writer.sep("(");
      expression.unparse(writer, 0, 0);
      writer.sep(")");
    }
  }
}
