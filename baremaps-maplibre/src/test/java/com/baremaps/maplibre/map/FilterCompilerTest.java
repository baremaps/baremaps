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

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.maplibre.expression.Expressions;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class FilterCompilerTest {

  private static String sql(String filter) {
    try {
      return FilterCompiler.sql(Expressions.read(filter), "tags");
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Test
  void has() {
    assertEquals("tags ? 'leisure'", sql("[\"has\", \"leisure\"]"));
  }

  @Test
  void notHas() {
    assertEquals("NOT (tags ? 'leisure')", sql("[\"!has\", \"leisure\"]"));
  }

  /**
   * The containment test is redundant to the meaning and decisive to the plan: without it the gin
   * index cannot answer the query and the scan goes sequential.
   */
  @Test
  void comparesAValueThroughTheIndex() {
    assertEquals("(tags ? 'waterway' AND tags ->> 'waterway' = 'river')",
        sql("[\"==\", [\"get\", \"waterway\"], \"river\"]"));
    assertEquals("(tags ? 'waterway' AND tags ->> 'waterway' IN ('river', 'stream'))",
        sql("[\"in\", [\"get\", \"waterway\"], [\"literal\", [\"river\", \"stream\"]]]"));
  }

  /**
   * A missing tag is not equal to anything, which is what the expression means and what a plain
   * {@code <>} against null does not say, so this one cannot carry the containment test.
   */
  @Test
  void inequalityHoldsForAMissingTag() {
    assertEquals("(tags ->> 'building' IS DISTINCT FROM 'no')",
        sql("[\"!=\", [\"get\", \"building\"], \"no\"]"));
  }

  @Test
  void comparesInEitherOrder() {
    assertEquals("(tags ? 'place' AND tags ->> 'place' = 'city')",
        sql("[\"==\", \"city\", [\"get\", \"place\"]]"));
  }

  @Test
  void combines() {
    assertEquals("(tags ? 'a' AND NOT (tags ? 'b'))",
        sql("[\"all\", [\"has\", \"a\"], [\"!\", [\"has\", \"b\"]]]"));
    assertEquals("(tags ? 'a' OR tags ? 'b')",
        sql("[\"any\", [\"has\", \"a\"], [\"has\", \"b\"]]"));
  }

  @Test
  void selectsEverythingWhenThereIsNoFilter() {
    assertNull(FilterCompiler.sql(null, "tags"));
  }

  @Test
  void quotesValuesThatCarryAQuote() {
    assertEquals("(tags ? 'name' AND tags ->> 'name' = 'l''Or')",
        sql("[\"==\", [\"get\", \"name\"], \"l'Or\"]"));
  }

  /** A predicate the language cannot say is refused by name, rather than mistranslated. */
  @Test
  void refusesWhatItCannotWrite() {
    var error = assertThrows(IllegalArgumentException.class,
        () -> sql("[\"within\", [\"get\", \"a\"]]"));
    assertTrue(error.getMessage().contains("within"), error.getMessage());
    assertTrue(error.getMessage().contains("where"), "it should point at the escape hatch");
  }

  @Test
  void refusesAComparisonThatIsNotOfATagAndALiteral() {
    var error = assertThrows(IllegalArgumentException.class,
        () -> sql("[\"==\", [\"get\", \"a\"], [\"get\", \"b\"]]"));
    assertTrue(error.getMessage().contains("where"), error.getMessage());
  }
}
