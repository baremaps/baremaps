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
import com.baremaps.maplibre.style.Style;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DemandTest {

  private static Style style(String layers) {
    var mapper = JsonMapper.builder()
        .addModule(Expressions.createModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build();
    try {
      return mapper.readValue("{\"version\":8,\"layers\":[" + layers + "]}", Style.class);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static String layer(String body) {
    return "{\"id\":\"l\",\"type\":\"fill\",\"source-layer\":\"s\"," + body + "}";
  }

  @Test
  void readsTheAttributesAFilterNames() {
    var style = style(layer("\"filter\":[\"==\",[\"get\",\"building\"],\"yes\"]"));
    assertEquals(Set.of("building"), Demand.of(style, 14).get("s").keys());
  }

  @Test
  void readsTheAttributesALayoutNames() {
    var style = style(layer("\"layout\":{\"text-field\":[\"get\",\"name\"]}"));
    assertEquals(Set.of("name"), Demand.of(style, 14).get("s").keys());
  }

  /**
   * A tile at zoom z is drawn for map zooms z up to z+1. Buildings fade in from an opacity of zero
   * at 13 to one at 13.5, so they are invisible at exactly 13 and needed just after it, and both
   * come from the z13 tile. Evaluating at the integer alone would drop them.
   */
  @Test
  void requiresALayerThatOnlyAppearsInsideTheZoomSpan() {
    var style = style(layer("\"filter\":[\"==\",[\"get\",\"building\"],\"yes\"],"
        + "\"paint\":{\"fill-opacity\":[\"interpolate\",[\"linear\"],[\"zoom\"],13,0,13.5,1]}"));
    assertTrue(Demand.of(style, 13).containsKey("s"), "needed for map zooms 13.5 to 14");
    assertFalse(Demand.of(style, 12).containsKey("s"), "invisible across the whole of z12");
  }

  @Test
  void dropsALayerThatIsHiddenAcrossTheWholeSpan() {
    var style = style(layer("\"paint\":{\"line-opacity\":0},\"type\":\"line\""));
    assertFalse(Demand.of(style, 14).containsKey("s"));
  }

  @Test
  void dropsALayerOutsideItsZoomRange() {
    var style = style(layer("\"minzoom\":14,\"filter\":[\"has\",\"shop\"]"));
    assertFalse(Demand.of(style, 12).containsKey("s"));
    assertTrue(Demand.of(style, 14).containsKey("s"));
  }

  /**
   * An expression that is not modelled has to widen the answer. Concluding that a layer reads
   * nothing would produce a tileset without the attributes it filters on, which renders as absence
   * rather than as an error.
   */
  @Test
  void readsThroughAnExpressionItDoesNotModel() {
    var style = style(layer("\"filter\":[\"in\",[\"get\",\"amenity\"],"
        + "[\"literal\",[\"bar\",\"cafe\"]]]"));
    assertTrue(Demand.of(style, 14).get("s").keys().contains("amenity"));
  }

  @Test
  void assumesALayerDrawsWhenItsOpacityCannotBeBounded() {
    var style = style(layer("\"filter\":[\"has\",\"shop\"],"
        + "\"paint\":{\"fill-opacity\":[\"case\",[\"has\",\"x\"],0,0]}"));
    assertTrue(Demand.of(style, 14).containsKey("s"));
  }

  /**
   * {@code !has} names its attribute as a bare string. Read as a literal it would leave the layer
   * appearing to read nothing.
   */
  @Test
  void readsTheAttributeOfANegatedHas() {
    var style = style(layer("\"filter\":[\"!has\",\"name\"]"));
    assertEquals(Set.of("name"), Demand.of(style, 14).get("s").keys());
  }

  @Test
  void acceptsALayerWithoutAFilter() {
    var style = style(layer("\"filter\":[]"));
    assertEquals(Set.of(), Demand.of(style, 14).get("s").keys());
  }

  @Test
  void boundsTheValuesComparedWithEquality() {
    var style = style(layer("\"filter\":[\"any\",[\"==\",[\"get\",\"power\"],\"plant\"],"
        + "[\"==\",[\"get\",\"power\"],\"substation\"]]"));
    var attributes = Demand.of(style, 14).get("s");
    assertTrue(attributes.isBounded("power"));
    assertEquals(Set.of("plant", "substation"), attributes.values().get("power"));
  }

  @Test
  void leavesTheValuesUnboundedWhenOneReadIsOpen() {
    var style = style(layer("\"filter\":[\"any\",[\"==\",[\"get\",\"building\"],\"yes\"],"
        + "[\"!=\",[\"get\",\"building\"],\"no\"]]"));
    var attributes = Demand.of(style, 14).get("s");
    assertTrue(attributes.keys().contains("building"));
    assertFalse(attributes.isBounded("building"), "an inequality admits any value");
  }

  @Test
  void mergesTheDemandOfEveryLayerOnASource() {
    var style = style("{\"id\":\"a\",\"type\":\"fill\",\"source-layer\":\"s\","
        + "\"filter\":[\"has\",\"building\"]},"
        + "{\"id\":\"b\",\"type\":\"symbol\",\"source-layer\":\"s\",\"minzoom\":15,"
        + "\"layout\":{\"text-field\":[\"get\",\"addr:housenumber\"]}}");
    assertEquals(Set.of("building"), Demand.of(style, 14).get("s").keys());
    assertEquals(Set.of("building", "addr:housenumber"), Demand.of(style, 15).get("s").keys());
  }
}
