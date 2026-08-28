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

package com.baremaps.maplibre.expression;

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.maplibre.expression.Expressions.*;
import com.baremaps.maplibre.vectortile.Feature;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExpressionsTest {

  @Test
  void literal() {
    assertEquals(1, new Literal(1).evaluate(null));
    assertEquals("value", new Literal("value").evaluate(null));
  }

  @Test
  void at() {
    var literal = new Literal(List.of(0, 1, 2));
    assertEquals(0, new At(0, literal).evaluate(null));
    assertEquals(1, new At(1, literal).evaluate(null));
    assertEquals(2, new At(2, literal).evaluate(null));
    assertEquals(null, new At(3, literal).evaluate(null));
    assertEquals(null, new At(-1, literal).evaluate(null));
  }

  @Test
  void get() {
    assertEquals("value",
        new Get("key").evaluate(new Feature(0L, Map.of("key", "value"), null)));
    assertEquals(null, new Get("key").evaluate(new Feature(0L, Map.of(), null)));
  }

  @Test
  void has() {
    assertEquals(true,
        new Has("key").evaluate(new Feature(0L, Map.of("key", "value"), null)));
    assertEquals(false, new Has("key").evaluate(new Feature(0L, Map.of(), null)));
  }

  @Test
  void inList() {
    var literal = new Literal(List.of(0, 1, 2));
    assertEquals(true, new In(0, literal).evaluate(null));
    assertEquals(true, new In(1, literal).evaluate(null));
    assertEquals(true, new In(2, literal).evaluate(null));
    assertEquals(false, new In(3, literal).evaluate(null));
  }

  @Test
  void inString() {
    var literal = new Literal("foobar");
    assertEquals(true, new In("foo", literal).evaluate(null));
    assertEquals(true, new In("bar", literal).evaluate(null));
    assertEquals(false, new In("baz", literal).evaluate(null));
  }

  @Test
  void indexOfList() {
    var literal = new Literal(List.of(0, 1, 2));
    assertEquals(0, new IndexOf(0, literal).evaluate(null));
    assertEquals(1, new IndexOf(1, literal).evaluate(null));
    assertEquals(2, new IndexOf(2, literal).evaluate(null));
    assertEquals(-1, new IndexOf(3, literal).evaluate(null));
  }

  @Test
  void indexOfString() {
    var literal = new Literal("foobar");
    assertEquals(0, new IndexOf("foo", literal).evaluate(null));
    assertEquals(3, new IndexOf("bar", literal).evaluate(null));
    assertEquals(-1, new IndexOf("baz", literal).evaluate(null));
  }

  @Test
  void lengthList() {
    var literal = new Literal(List.of(0, 1, 2));
    assertEquals(3, new Length(literal).evaluate(null));
  }

  @Test
  void lengthString() {
    var literal = new Literal("foo");
    assertEquals(3, new Length(literal).evaluate(null));
  }

  @Test
  void lengthNull() {
    var literal = new Literal(null);
    assertEquals(-1, new Length(literal).evaluate(null));
  }

  @Test
  void slice() {
    var literal = new Literal("foobar");
    assertEquals("foobar", new Slice(literal, new Literal(0)).evaluate(null));
    assertEquals("bar", new Slice(literal, new Literal(3)).evaluate(null));
    assertEquals("foo", new Slice(literal, new Literal(0), new Literal(3)).evaluate(null));
    assertEquals("bar", new Slice(literal, new Literal(3), new Literal(6)).evaluate(null));
  }

  @Test
  void not() throws IOException {
    assertEquals(true, Expressions.read("[\"!\", false]").evaluate(null));
    assertEquals(false, Expressions.read("[\"!\", true]").evaluate(null));
  }

  @Test
  void notEqual() throws IOException {
    assertEquals(true, Expressions.read("[\"!=\", 1, 2]").evaluate(null));
    assertEquals(false, Expressions.read("[\"!=\", 1, 1]").evaluate(null));
  }

  @Test
  void less() throws IOException {
    assertEquals(true, Expressions.read("[\"<\", 1, 2]").evaluate(null));
    assertEquals(false, Expressions.read("[\"<\", 1, 1]").evaluate(null));
    assertEquals(false, Expressions.read("[\"<\", 1, 0]").evaluate(null));
  }

  @Test
  void lessOrEqual() throws IOException {
    assertEquals(true, Expressions.read("[\"<=\", 1, 2]").evaluate(null));
    assertEquals(true, Expressions.read("[\"<=\", 1, 1]").evaluate(null));
    assertEquals(false, Expressions.read("[\"<=\", 1, 0]").evaluate(null));
  }

  @Test
  void equal() throws IOException {
    assertEquals(true, Expressions.read("[\"==\", 1, 1]").evaluate(null));
    assertEquals(false, Expressions.read("[\"==\", 1, 2]").evaluate(null));
  }

  @Test
  void greater() throws IOException {
    assertEquals(true, Expressions.read("[\">\", 1, 0]").evaluate(null));
    assertEquals(false, Expressions.read("[\">\", 1, 1]").evaluate(null));
    assertEquals(false, Expressions.read("[\">\", 1, 2]").evaluate(null));
  }

  @Test
  void greaterOrEqual() throws IOException {
    assertEquals(true, Expressions.read("[\">=\", 1, 0]").evaluate(null));
    assertEquals(true, Expressions.read("[\">=\", 1, 1]").evaluate(null));
    assertEquals(false, Expressions.read("[\">=\", 1, 2]").evaluate(null));
  }

  @Test
  void all() {
    assertEquals(true, new All(List.of(new Literal(true), new Literal(true))).evaluate(null));
    assertEquals(false, new All(List.of(new Literal(true), new Literal(false))).evaluate(null));
    assertEquals(false, new All(List.of(new Literal(false), new Literal(false))).evaluate(null));
    assertEquals(true, new All(List.of()).evaluate(null));
  }

  @Test
  void any() {
    assertEquals(true, new Any(List.of(new Literal(true), new Literal(true))).evaluate(null));
    assertEquals(true, new Any(List.of(new Literal(true), new Literal(false))).evaluate(null));
    assertEquals(false, new Any(List.of(new Literal(false), new Literal(false))).evaluate(null));
    assertEquals(false, new Any(List.of()).evaluate(null));
  }

  @Test
  void caseExpression() {
    assertEquals("a",
        new Case(new Literal(true), new Literal("a"), new Literal("b")).evaluate(null));
    assertEquals("b",
        new Case(new Literal(false), new Literal("a"), new Literal("b")).evaluate(null));
  }

  @Test
  void coalesce() {
    assertEquals("a", new Coalesce(List.of(new Literal(null), new Literal("a"), new Literal("b")))
        .evaluate(null));
    assertEquals("b", new Coalesce(List.of(new Literal(null), new Literal("b"), new Literal("a")))
        .evaluate(null));
    assertEquals(null, new Coalesce(List.of(new Literal(null))).evaluate(null));
    assertEquals(null, new Coalesce(List.of()).evaluate(null));
  }

  @Test
  void match() throws IOException {
    assertEquals("foo", Expressions
        .read("[\"match\", \"foo\", \"foo\", \"foo\", \"bar\", \"bar\", \"baz\"]").evaluate(null));
    assertEquals("bar", Expressions
        .read("[\"match\", \"bar\", \"foo\", \"foo\", \"bar\", \"bar\", \"baz\"]").evaluate(null));
    assertEquals("baz", Expressions
        .read("[\"match\", \"baz\", \"foo\", \"foo\", \"bar\", \"bar\", \"baz\"]").evaluate(null));
  }

}
