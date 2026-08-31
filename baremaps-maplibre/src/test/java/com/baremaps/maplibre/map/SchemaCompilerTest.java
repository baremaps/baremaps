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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

class SchemaCompilerTest {

  private static MapSpec spec(String generalize) {
    var mapper = JsonMapper.builder()
        .addModule(Expressions.createModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build();
    try {
      return mapper.readValue("""
          {"name":"test","minzoom":0,"maxzoom":16,"layers":[
            {"id":"a","type":"fill","sourceLayer":"leisure",
             "sourceQueries":[{"minzoom":1,"maxzoom":20,"from":"osm_leisure"}],
             "filter":["has","leisure"],
             "generalize":%s}]}
          """.formatted(generalize), MapSpec.class);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static final String LEISURE =
      "{\"by\":\"leisure\",\"values\":[\"park\",\"pitch\"]}";

  @Test
  void buildsOneViewPerGeneralizedZoom() {
    var sql = SchemaCompiler.sql(spec(LEISURE));
    for (int zoom = 1; zoom <= 12; zoom++) {
      assertTrue(
          sql.contains("CREATE MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z" + zoom + " AS"),
          "missing zoom " + zoom);
    }
    assertFalse(sql.contains("osm_leisure_z13"), "zoom 13 reads the features themselves");
  }

  /**
   * Each level reads the one above it, so the order it is written in is the order it must run in.
   */
  @Test
  void buildsTheLevelsFromTheTopDown() {
    var sql = SchemaCompiler.sql(spec(LEISURE));
    assertTrue(sql.indexOf("osm_leisure_z12 AS") < sql.indexOf("osm_leisure_z11 AS"));
    assertTrue(sql.contains("FROM osm_leisure\n"), "the highest level reads the features");
    assertTrue(sql.contains("FROM osm_leisure_z12\n"), "the next one reads the level above");
  }

  /** Only the first level chooses values; below it, what is left was already chosen. */
  @Test
  void choosesValuesOnlyAtTheFirstLevel() {
    var sql = SchemaCompiler.sql(spec(LEISURE));
    assertEquals(1, sql.split("tags ->> 'leisure' IN \\(", -1).length - 1);
    assertTrue(sql.contains("IN ('park', 'pitch')"));
  }

  @Test
  void mergesEverythingWhenNoValueIsNamed() {
    var sql = SchemaCompiler.sql(spec("{\"by\":\"amenity\"}"));
    assertFalse(sql.contains(" IN ("), "no value named means no value filter");
    assertTrue(sql.contains("jsonb_build_object('amenity', tag)"));
  }

  /** Below the merge a cluster would span a continent, so the geometry is only simplified. */
  @Test
  void stopsMergingBelowTheMergeZoom() {
    var sql = SchemaCompiler.sql(spec(LEISURE));
    var z4 = sql.indexOf("osm_leisure_z4 AS");
    var z3 = sql.indexOf("osm_leisure_z3 AS");
    assertTrue(sql.substring(z4, z3).contains("st_clusterdbscan"));
    assertFalse(sql.substring(z3).contains("st_clusterdbscan"));
    assertTrue(sql.substring(z3).contains("SELECT id, tags, st_simplifypreservetopology"));
  }

  /** The threshold relaxes below the merge, where 32 tolerances squared exceeds most countries. */
  @Test
  void relaxesTheAreaThresholdBelowTheMerge() {
    var sql = SchemaCompiler.sql(spec(LEISURE));
    assertTrue(sql.contains("POWER(78270 / POWER(2, 4), 2) * 32"));
    assertTrue(sql.contains("POWER(78270 / POWER(2, 3), 2) * 16"));
  }

  @Test
  void honoursDeclaredThresholds() {
    var sql = SchemaCompiler.sql(spec(
        "{\"by\":\"leisure\",\"below\":10,\"mergeAbove\":6,\"area\":8,\"buffer\":2.0}"));
    assertTrue(sql.contains("osm_leisure_z9 AS"));
    assertFalse(sql.contains("osm_leisure_z10 AS"));
    assertTrue(sql.contains("POWER(78270 / POWER(2, 9), 2) * 8"));
    assertTrue(sql.contains("78270 / POWER(2, 9) * 2"), "whole numbers lose the point");
    assertFalse(sql.substring(sql.indexOf("osm_leisure_z5 AS")).contains("st_clusterdbscan"));
  }

  @Test
  void indexesEveryLevel() {
    var sql = SchemaCompiler.sql(spec(LEISURE));
    assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS osm_leisure_z12_geom_idx "
        + "ON osm_leisure_z12 USING GIST(geom)"));
    assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS osm_leisure_z12_tags_idx "
        + "ON osm_leisure_z12 USING GIN(tags)"));
  }

  // --- lines ----------------------------------------------------------------

  private static MapSpec lines(String generalize) {
    var mapper = JsonMapper.builder()
        .addModule(Expressions.createModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build();
    try {
      return mapper.readValue("""
          {"name":"test","minzoom":0,"maxzoom":16,"layers":[
            {"id":"a","type":"line","sourceLayer":"highway",
             "sourceQueries":[{"minzoom":4,"maxzoom":20,"from":"osm_highway"}],
             "filter":["has","highway"],
             "generalize":%s}]}
          """.formatted(generalize), MapSpec.class);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static final String HIGHWAY = """
      {"kind":"lines","by":"highway","values":["motorway","trunk","residential"],
       "minzoom":{"motorway":4,"trunk":6,"residential":11}}""";

  /**
   * Line work is merged once rather than level by level: two roads that meet are already one line
   * after the first pass, so merging again gains nothing.
   */
  @Test
  void mergesLineWorkOnceAndSimplifiesOutOfIt() {
    var sql = SchemaCompiler.sql(lines(HIGHWAY));
    assertEquals(1, sql.split("ST_LineMerge", -1).length - 1, "merged once");
    assertTrue(sql.contains("CREATE MATERIALIZED VIEW IF NOT EXISTS osm_highway_simplified AS"));
    for (int zoom = 4; zoom <= 12; zoom++) {
      assertTrue(sql.contains("osm_highway_z" + zoom + " AS"), "missing zoom " + zoom);
      assertTrue(sql.contains("FROM osm_highway_simplified"), "every level reads the merge");
    }
  }

  @Test
  void stopsTheChainWhereTheLayerStopsBeingQueried() {
    var sql = SchemaCompiler.sql(lines(HIGHWAY));
    assertFalse(sql.contains("osm_highway_z3 AS"), "the layer is not queried below zoom 4");
  }

  @Test
  void dropsLinesTooShortToSee() {
    assertTrue(SchemaCompiler.sql(lines(HIGHWAY))
        .contains("st_length(geom) > 78270 / POWER(2, 8) * 2"));
  }

  /** A motorway is worth a pixel at zoom 4 and a residential street is not. */
  @Test
  void narrowsTheClassesAsTheMapZoomsOut() {
    var sql = SchemaCompiler.sql(lines(HIGHWAY));
    var z12 = sql.substring(sql.indexOf("osm_highway_z12 AS"), sql.indexOf("osm_highway_z11 AS"));
    var z10 = sql.substring(sql.indexOf("osm_highway_z10 AS"), sql.indexOf("osm_highway_z9 AS"));
    var z5 = sql.substring(sql.indexOf("osm_highway_z5 AS"), sql.indexOf("osm_highway_z4 AS"));

    // Every class still qualifies at 12, and the merge holds those and no others, so no test.
    assertFalse(z12.contains("tags ->> 'highway' IN"), "a test that keeps everything is only work");
    assertTrue(z10.contains("IN ('motorway', 'trunk')"));
    assertTrue(z5.contains("IN ('motorway')"));
  }

  @Test
  void indexesTheMergeAndEveryLevel() {
    var sql = SchemaCompiler.sql(lines(HIGHWAY));
    assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS osm_highway_simplified_geom "
        + "ON osm_highway_simplified USING GIST(geom)"));
    assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS osm_highway_z8_geom_idx "
        + "ON osm_highway_z8 USING GIST(geom)"));
  }

  @Test
  void writesNothingForALayerThatIsNotGeneralized() {
    var mapper = JsonMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();
    try {
      var spec = mapper.readValue("""
          {"name":"test","layers":[{"id":"a","type":"fill","sourceLayer":"building",
           "sourceQueries":[{"minzoom":13,"maxzoom":20,"from":"osm_building"}]}]}
          """, MapSpec.class);
      assertEquals("", SchemaCompiler.sql(spec));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
