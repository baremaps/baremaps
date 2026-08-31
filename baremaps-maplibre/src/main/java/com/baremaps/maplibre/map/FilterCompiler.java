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

package com.baremaps.maplibre.map;


import com.baremaps.maplibre.expression.Expressions.All;
import com.baremaps.maplibre.expression.Expressions.Any;
import com.baremaps.maplibre.expression.Expressions.Equal;
import com.baremaps.maplibre.expression.Expressions.Expression;
import com.baremaps.maplibre.expression.Expressions.Get;
import com.baremaps.maplibre.expression.Expressions.Greater;
import com.baremaps.maplibre.expression.Expressions.GreaterOrEqual;
import com.baremaps.maplibre.expression.Expressions.Has;
import com.baremaps.maplibre.expression.Expressions.In;
import com.baremaps.maplibre.expression.Expressions.Less;
import com.baremaps.maplibre.expression.Expressions.LessOrEqual;
import com.baremaps.maplibre.expression.Expressions.Literal;
import com.baremaps.maplibre.expression.Expressions.Not;
import com.baremaps.maplibre.expression.Expressions.NotEqual;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns a filter expression into a sql predicate over a jsonb column of tags.
 *
 * <p>
 * A filter is written in the same language the style filters in, so the person describing the map
 * writes one kind of expression rather than an expression and a dialect. What that expression has
 * to become in order to run quickly is this class's problem.
 *
 * <p>
 * It is a real problem rather than a formality. The obvious reading of {@code has} is
 * {@code tags ->> 'k' IS NOT NULL}, which cannot use the gin index on the column and turns a bitmap
 * scan into a sequential one; measured against the ways of a country extract, that is the
 * difference between a plan costing 130 thousand and one costing 381 thousand. So every test of a
 * value is emitted with a containment test beside it: {@code tags ? 'k' AND tags ->> 'k' = 'v'}.
 * The containment is redundant to the meaning and decisive to the plan, and it is exactly the kind
 * of thing a hand-written clause forgets.
 */
public final class FilterCompiler {

  private FilterCompiler() {}

  /**
   * Returns the sql predicate a filter expression describes, or null when the filter selects
   * everything.
   *
   * @param expression the filter
   * @param column the jsonb column holding the tags
   * @return the predicate
   */
  public static String sql(Expression expression, String column) {
    if (expression == null || expression instanceof Literal) {
      return null;
    }
    return predicate(expression, column);
  }

  private static String predicate(Expression expression, String column) {
    if (expression instanceof Has(String key)) {
      return contains(column, key);
    }
    if (expression instanceof Not(Expression negated)) {
      return "NOT (%s)".formatted(predicate(negated, column));
    }
    if (expression instanceof All(List<Expression> expressions)) {
      return join(expressions, "AND", column, "TRUE");
    }
    if (expression instanceof Any(List<Expression> expressions)) {
      return join(expressions, "OR", column, "FALSE");
    }
    if (expression instanceof Equal(Expression left, Expression right)) {
      return comparison(left, right, "=", column, true);
    }
    if (expression instanceof NotEqual(Expression left, Expression right)) {
      // A missing tag is not equal to anything, which is what IS DISTINCT FROM says and what a
      // plain <> does not, so this one cannot carry the containment test.
      return comparison(left, right, "IS DISTINCT FROM", column, false);
    }
    if (expression instanceof Less(Expression left, Expression right)) {
      return comparison(left, right, "<", column, true);
    }
    if (expression instanceof LessOrEqual(Expression left, Expression right)) {
      return comparison(left, right, "<=", column, true);
    }
    if (expression instanceof Greater(Expression left, Expression right)) {
      return comparison(left, right, ">", column, true);
    }
    if (expression instanceof GreaterOrEqual(Expression left, Expression right)) {
      return comparison(left, right, ">=", column, true);
    }
    if (expression instanceof In(Expression needle, Expression haystack)) {
      return within(needle, haystack, column);
    }
    throw new IllegalArgumentException(String.format(
        "The filter cannot be written as sql: '%s'. Use where for a predicate this does not cover.",
        expression.name()));
  }

  private static String join(List<Expression> expressions, String operator, String column,
      String empty) {
    if (expressions.isEmpty()) {
      return empty;
    }
    return "(" + expressions.stream().map(child -> predicate(child, column))
        .collect(Collectors.joining(" " + operator + " ")) + ")";
  }

  /** A comparison between a tag and a literal, in either order. */
  private static String comparison(Expression left, Expression right, String operator,
      String column, boolean indexed) {
    var key = key(left, right);
    var value = literal(left, right);
    if (key == null || value == null) {
      throw new IllegalArgumentException(
          "A filter compares something other than a tag and a literal, which sql cannot express "
              + "here; use where instead.");
    }
    var test = "%s ->> %s %s %s".formatted(column, quote(key), operator, quote(value));
    return indexed ? "(%s AND %s)".formatted(contains(column, key), test) : "(%s)".formatted(test);
  }

  private static String within(Expression needle, Expression haystack, String column) {
    if (!(needle instanceof Get(String key)) || !(haystack instanceof Literal(Object value))
        || !(value instanceof List<?> values)) {
      throw new IllegalArgumentException(
          "in expects a tag and a literal list, as in [\"in\", [\"get\", \"k\"], "
              + "[\"literal\", [\"a\", \"b\"]]].");
    }
    var list = values.stream().map(String::valueOf).map(FilterCompiler::quote)
        .collect(Collectors.joining(", "));
    return "(%s AND %s ->> %s IN (%s))".formatted(contains(column, key), column, quote(key), list);
  }

  /** The containment test that lets the gin index answer the query. */
  private static String contains(String column, String key) {
    return "%s ? %s".formatted(column, quote(key));
  }

  private static String key(Expression left, Expression right) {
    if (left instanceof Get(String key)) {
      return key;
    }
    if (right instanceof Get(String key)) {
      return key;
    }
    return null;
  }

  private static String literal(Expression left, Expression right) {
    if (left instanceof Get && right instanceof Literal(Object value) && value != null) {
      return String.valueOf(value);
    }
    if (right instanceof Get && left instanceof Literal(Object value) && value != null) {
      return String.valueOf(value);
    }
    return null;
  }

  private static String quote(String value) {
    return "'" + value.replace("'", "''") + "'";
  }
}
