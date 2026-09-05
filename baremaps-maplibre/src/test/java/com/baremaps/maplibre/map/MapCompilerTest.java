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
import com.baremaps.maplibre.tileset.Tileset;
import com.baremaps.maplibre.tileset.TilesetQuery;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MapCompilerTest {

  /** The queries that produce the building source layer, as one layer declares them. */
  private static final String BUILDING_SOURCE =
      "\"sourceQueries\":[{\"minzoom\":13,\"maxzoom\":20,\"from\":\"osm_building\"}]";

  private static MapSpec spec(String layers) {
    return spec(layers, false);
  }

  private static MapSpec spec(String layers, boolean featureIds) {
    return read("""
        {"name":"test","source":{"minzoom":0,"maxzoom":16,"featureIds":%s},"layers":[%s]}
        """.formatted(featureIds, layers));
  }

  /** Reads a whole map document, the way the configuration reader does. */
  private static MapSpec read(String document) {
    var mapper = JsonMapper.builder()
        .addModule(Expressions.createModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build();
    try {
      return mapper.readValue(document, MapSpec.class);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  /** A fill layer on a source layer, with whatever extra members the test needs. */
  private static String layer(String id, String source, String members) {
    return "{\"id\":\"%s\",\"type\":\"fill\",\"sourceLayer\":\"%s\"%s}"
        .formatted(id, source, members.isEmpty() ? "" : "," + members);
  }

  private static String building(String members) {
    return layer("a", "building", BUILDING_SOURCE + ",\"filter\":[\"has\",\"building\"]"
        + (members.isEmpty() ? "" : "," + members));
  }

  private static List<TilesetQuery> queries(Tileset tileset, String id) {
    return MapCompiler.layers(tileset).get(id).getQueries();
  }

  @Test
  void selectsOnlyTheAttributesTheStyleReads() {
    var queries = queries(MapCompiler.tileset(spec(building(""))), "building");
    assertEquals(1, queries.size());
    assertEquals("SELECT jsonb_build_object('building', tags -> 'building') AS tags, "
        + "geom AS geom FROM osm_building", queries.get(0).getSql());
  }

  /**
   * A query is declared over a range, but what the style reads changes inside it. The range is
   * split where the demand changes so that the attributes follow the style rather than the
   * declaration.
   */
  @Test
  void splitsAQueryWhereTheDemandChanges() {
    var queries = queries(MapCompiler.tileset(spec(building("") + ","
        + "{\"id\":\"b\",\"type\":\"symbol\",\"sourceLayer\":\"building\",\"minzoom\":15,"
        + "\"layout\":{\"text-field\":[\"get\",\"addr:housenumber\"]}}")), "building");
    assertEquals(2, queries.size());

    assertEquals(13, queries.get(0).getMinzoom());
    assertEquals(15, queries.get(0).getMaxzoom());
    assertFalse(queries.get(0).getSql().contains("addr:housenumber"));

    assertEquals(15, queries.get(1).getMinzoom());
    assertTrue(queries.get(1).getSql().contains("addr:housenumber"));
  }

  @Test
  void dropsTheZoomLevelsWhereNothingIsDrawn() {
    var queries = queries(MapCompiler.tileset(spec(building("\"minzoom\":14"))), "building");
    assertEquals(1, queries.size());
    assertEquals(14, queries.get(0).getMinzoom(), "no layer draws building below 14");
  }

  @Test
  void keepsTheDeclaredCondition() {
    var spec = spec(layer("a", "leisure",
        "\"sourceQueries\":[{\"minzoom\":13,\"maxzoom\":20,\"from\":\"osm_leisure\","
            + "\"where\":\"tags ? 'leisure'\"}],\"filter\":[\"has\",\"leisure\"]"));
    assertTrue(queries(MapCompiler.tileset(spec), "leisure").get(0).getSql()
        .endsWith("FROM osm_leisure WHERE tags ? 'leisure'"));
  }

  /** Simplification is a judgement about the map, so it stays declared rather than derived. */
  @Test
  void appliesTheDeclaredSimplification() {
    var spec = spec(layer("a", "building",
        "\"sourceQueries\":[{\"minzoom\":13,\"maxzoom\":20,\"from\":\"osm_building\","
            + "\"simplify\":0.5}],\"filter\":[\"has\",\"building\"]"));
    assertTrue(queries(MapCompiler.tileset(spec), "building").get(0).getSql()
        .contains("ST_SimplifyPreserveTopology(geom, 78270 / POWER(2, $zoom) * 0.5)"));
  }

  @Test
  void selectsNoAttributesWhenTheStyleReadsNone() {
    var spec = spec(layer("a", "ocean",
        "\"sourceQueries\":[{\"minzoom\":0,\"maxzoom\":10,\"from\":\"osm_ocean\"}],"
            + "\"paint\":{\"fill-color\":\"#00f\"}"));
    assertTrue(queries(MapCompiler.tileset(spec), "ocean").get(0).getSql()
        .startsWith("SELECT '{}'::jsonb AS tags"));
  }

  /**
   * An attribute nothing draws with reaches the tile because a layer asks for it, and reaches it
   * the same way every other attribute does: through the projection, at the zoom levels the layer
   * asking for it draws at.
   */
  @Test
  void carriesTheAttributesALayerAsksFor() {
    var queries =
        queries(MapCompiler.tileset(spec(building("\"attributes\":[\"osm_id\"]"))), "building");
    assertEquals(1, queries.size());
    assertEquals("SELECT jsonb_build_object('building', tags -> 'building', "
        + "'osm_id', tags -> 'osm_id') AS tags, geom AS geom FROM osm_building",
        queries.get(0).getSql());
  }

  @Test
  void carriesAnAttributeOnlyWhereTheLayerAskingForItDraws() {
    var queries = queries(MapCompiler.tileset(spec(building("") + ","
        + layer("b", "building", "\"minzoom\":15,\"attributes\":[\"osm_id\"],"
            + "\"paint\":{\"fill-color\":\"#000\"}"))),
        "building");
    assertEquals(2, queries.size());
    assertFalse(queries.get(0).getSql().contains("osm_id"));
    assertEquals(15, queries.get(1).getMinzoom());
    assertTrue(queries.get(1).getSql().contains("osm_id"));
  }

  /**
   * A feature carrying only an identifier is still drawn as nothing, so a carried attribute cannot
   * be what admits it. An identifier every feature has would otherwise admit every feature and
   * leave the test with nothing to drop.
   */
  @Test
  void doesNotAdmitAFeatureByAnAttributeNothingDrawsWith() {
    var spec = spec(layer("a", "point",
        "\"sourceQueries\":[{\"minzoom\":13,\"maxzoom\":20,\"from\":\"osm_point\","
            + "\"drawable\":true}],\"attributes\":[\"osm_id\"],"
            + "\"filter\":[\"has\",\"amenity\"]"));
    var sql = queries(MapCompiler.tileset(spec), "point").get(0).getSql();
    assertTrue(sql.contains("'osm_id', tags -> 'osm_id'"), "carried into the projection");
    assertTrue(sql.endsWith("WHERE (tags ? 'amenity')"), "and left out of the drawable test");
  }

  @Test
  void publishesTheDerivedAttributesAsFields() {
    assertEquals(Set.of("building"), MapCompiler.layers(MapCompiler.tileset(spec(building(""))))
        .get("building").getFields().keySet());
  }

  /**
   * A style cannot draw with an identifier, and identifiers are unique, so each one becomes its own
   * entry in a tile's value dictionary. A derived tileset leaves them out unless asked for them.
   */
  @Test
  void omitsFeatureIdentifiersUnlessAskedForThem() {
    var derived = MapCompiler.tileset(spec(building("")));
    assertFalse(derived.isFeatureIds());
    assertFalse(queries(derived, "building").get(0).getSql().startsWith("SELECT id,"));

    var asked = MapCompiler.tileset(spec(building(""), true));
    assertTrue(asked.isFeatureIds());
    assertTrue(queries(asked, "building").get(0).getSql().startsWith("SELECT id,"));
  }

  // --- what the compiler fills in ------------------------------------------

  @Test
  void fillsInTheSourceEveryLayerReads() {
    var style = MapCompiler.style(spec(building("")));
    assertEquals("baremaps", style.getLayers().get(0).getSource());
    assertEquals(Set.of("baremaps"), style.getSources().keySet());
    assertEquals(8, style.getVersion());
  }

  @Test
  void startsALayerWhereTheFeaturesItReadsBegin() {
    // The second layer declares nothing: it reads the building source layer, so it begins where
    // the queries for that source layer begin rather than at zoom zero.
    var style = MapCompiler.style(spec(building("") + "," + layer("b", "building", "")));
    assertEquals(13, style.getLayers().get(0).getMinzoom());
    assertEquals(13, style.getLayers().get(1).getMinzoom());
  }

  @Test
  void keepsTheZoomALayerNamesForItself() {
    var style = MapCompiler.style(spec(building("\"minzoom\":16")));
    assertEquals(16, style.getLayers().get(0).getMinzoom(),
        "a layer starting later than its data does is a decision, not an omission");
  }

  @Test
  void leavesALayerReadingNoSourceLayerAlone() {
    var style = MapCompiler.style(spec(building("") + ","
        + "{\"id\":\"b\",\"type\":\"background\"}"));
    assertNull(style.getLayers().get(1).getMinzoom());
  }

  @Test
  void dropsTheTilesetExtensionsFromTheStyle() throws Exception {
    var style = MapCompiler.style(spec(building("")));
    var json = JsonMapper.builder().build().writeValueAsString(style);
    assertFalse(json.contains("sourceQueries"), "the renderer has no use for the queries");
    assertFalse(json.contains("sourceSchema"));
  }

  // --- what it refuses -----------------------------------------------------

  @Test
  void refusesASourceLayerDeclaredTwice() {
    var spec = spec(building("") + "," + layer("b", "building", BUILDING_SOURCE));
    var error = assertThrows(IllegalArgumentException.class, () -> MapCompiler.tileset(spec));
    assertTrue(error.getMessage().contains("declared twice"), error.getMessage());
    assertTrue(error.getMessage().contains("'a'") && error.getMessage().contains("'b'"));
  }

  @Test
  void refusesASourceLayerNoLayerDeclares() {
    var spec = spec(layer("a", "building", "\"filter\":[\"has\",\"building\"]"));
    var error = assertThrows(IllegalArgumentException.class, () -> MapCompiler.tileset(spec));
    assertTrue(error.getMessage().contains("No layer says where"), error.getMessage());
  }

  /**
   * A layer pasted from a style carries the specification's spelling. Ignoring it would leave the
   * layer with no source layer and nothing to draw, quietly, so it is refused.
   */
  @Test
  void refusesTheSpecificationSpellingOfSourceLayer() {
    var error = assertThrows(Exception.class,
        () -> spec("{\"id\":\"a\",\"type\":\"fill\",\"source-layer\":\"building\"}"));
    var message = error.getCause() == null ? error.getMessage() : error.getCause().getMessage();
    assertTrue(message.contains("sourceLayer"), message);
  }

  /**
   * The tile properties moved into the source, and a map that still declares them at the top level
   * would otherwise load, ignore them and cover the default zoom range from nowhere.
   */
  @Test
  void refusesTheTilePropertiesAtTheTopLevel() {
    var error = assertThrows(Exception.class, () -> read("""
        {"name":"test","tilejson":"http://localhost/tiles.json","layers":[]}
        """));
    var message = error.getCause() == null ? error.getMessage() : error.getCause().getMessage();
    assertTrue(message.contains("source.url"), message);
  }

  /** An omitted source is the defaults, so that reading the format tells the whole story. */
  @Test
  void fillsInTheSourceDefaults() {
    var source = read("{\"name\":\"test\",\"layers\":[]}").source();
    assertEquals("baremaps", source.id());
    assertEquals(0, source.minzoom());
    assertEquals(20, source.maxzoom());
    assertFalse(source.featureIds());
  }
}
