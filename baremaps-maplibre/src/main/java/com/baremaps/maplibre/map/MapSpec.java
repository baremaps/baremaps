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


import com.baremaps.maplibre.expression.Expressions.Expression;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * A map described once: a few properties of the map as a whole, and its layers in the order they
 * are painted.
 *
 * <p>
 * A layer says how it is drawn and, for one layer per source layer, where its features come from.
 * What it does not say is anything {@link MapCompiler} can work out: the source it reads, the
 * attributes the tiles carry, and the zoom levels a query is worth running at follow from the
 * layers themselves, and writing them down again is how they drift apart.
 *
 * <p>
 * There is one list and it is ordered, because paint order is global and this basemap interleaves
 * its subjects deliberately, drawing every background before any overlay and stacking tunnels,
 * buildings, roads and bridges in that sequence. Fifty layers form thirty-four runs of a single
 * subject, so the order cannot be recovered from any per-subject grouping without changing which
 * features cover which.
 *
 * @param name the name of the map
 * @param center the longitude and latitude the map opens at
 * @param zoom the zoom level the map opens at
 * @param bounds the extent tiles are produced for
 * @param attribution the attribution shown for the data
 * @param sprite the URL of the icon sprite
 * @param glyphs the URL template of the fonts
 * @param source the name the style refers to the tiles by; defaults to {@code baremaps}
 * @param tilejson the URL the style reads the tileset from
 * @param tiles the URL templates the tiles are served from
 * @param database the database the queries are run against
 * @param minzoom the lowest zoom level tiles are produced for; defaults to 0
 * @param maxzoom the highest zoom level tiles are produced for; defaults to 20
 * @param featureIds whether the tiles carry the identifier of each feature; off by default, because
 *        a style cannot draw with one, and identifiers are unique and so compress badly
 * @param schema the sql that has to run before the layers' own, in order: the extensions and
 *        functions the queries rely on, and the tables the sources are read out of
 * @param layers the layers, in the order they are painted
 */
public record MapSpec(
    @JsonProperty("name") String name,
    @JsonProperty("center") List<BigDecimal> center,
    @JsonProperty("zoom") BigDecimal zoom,
    @JsonProperty("bounds") List<Double> bounds,
    @JsonProperty("attribution") String attribution,
    @JsonProperty("sprite") String sprite,
    @JsonProperty("glyphs") String glyphs,
    @JsonProperty("source") String source,
    @JsonProperty("tilejson") String tilejson,
    @JsonProperty("tiles") List<String> tiles,
    @JsonProperty("database") Object database,
    @JsonProperty("minzoom") Integer minzoom,
    @JsonProperty("maxzoom") Integer maxzoom,
    @JsonProperty("feature_ids") Boolean featureIds,
    @JsonProperty("schema") List<String> schema,
    @JsonProperty("layers") List<MapLayer> layers) {

  /**
   * How a source layer is thinned out as the map zooms away.
   *
   * <p>
   * Below {@code below}, neighbouring areas carrying the same value are dilated until they touch,
   * merged, eroded back by the same amount and simplified. Dilating and eroding by the same
   * distance is a morphological closing, which keeps the result from drifting outwards as the chain
   * descends. Each level reads the one above it, so the work is done once per level rather than
   * once per tile.
   *
   * <p>
   * What survives, and how much detail it loses, is a judgement about the map: a value worth
   * drawing at zoom 14 is not necessarily worth a pixel at zoom 4. The engine owns the views this
   * produces, their names and the order they are built and refreshed in; it does not own this
   * decision.
   *
   * @param below the first zoom level that is generalized, counting down; defaults to 13
   * @param by the attribute whose value neighbours must share to be merged
   * @param values the values worth showing once the map is generalized
   * @param mergeAbove the lowest zoom level that still merges neighbours; below it the geometry is
   *        only simplified, because at that scale a merge spans a continent. Defaults to 4
   * @param area the area a feature must keep to survive, in squared simplification tolerances;
   *        defaults to 32
   * @param areaBelowMerge the same threshold below {@code mergeAbove}, relaxed because 32
   *        tolerances squared is larger than most countries at those scales; defaults to 16
   * @param buffer how far neighbours are dilated to find each other, in tolerances; defaults to 1.1
   * @param kind {@code areas}, the default, or {@code lines}. Areas are merged level by level, each
   *        reading the one above it. Lines are merged once, into a single relation every level then
   *        simplifies out of, because merging line work repeatedly gains nothing
   * @param minzoom the lowest zoom each value is drawn at, for values that stop being drawn before
   *        the bottom of the chain: a motorway is worth a pixel at zoom 4 and a residential street
   *        is not
   * @param length the length a line must keep to survive, in simplification tolerances; defaults to
   *        2
   */
  public record Generalize(
      @JsonProperty("below") Integer below,
      @JsonProperty("by") String by,
      @JsonProperty("values") List<String> values,
      @JsonProperty("kind") String kind,
      @JsonProperty("minzoom") java.util.Map<String, Integer> minzoom,
      @JsonProperty("length") Double length,
      @JsonProperty("mergeAbove") Integer mergeAbove,
      @JsonProperty("area") Double area,
      @JsonProperty("areaBelowMerge") Double areaBelowMerge,
      @JsonProperty("buffer") Double buffer) {
  }

  /**
   * Where the features of a source layer come from over a range of zoom levels.
   *
   * <p>
   * The relation and the condition are declared; the selected attributes are not, because they
   * follow from the layers that read them. Simplification does not follow from anything and stays
   * declared: how much detail a feature can lose before it looks wrong is a judgement about the
   * map, not a fact about the style.
   *
   * @param minzoom the first zoom level this query applies to; defaults to the map's
   * @param maxzoom the first zoom level beyond it; defaults to one past the map's
   * @param from the relation to select from, which may be a subquery
   * @param filter an optional condition, written in the language the style filters in
   * @param where an optional condition written as sql, for the rare predicate a filter cannot say
   * @param simplify an optional simplification tolerance, in tile pixels
   * @param drawable whether to drop features carrying none of the attributes the style reads at
   *        that zoom, which can only be drawn as nothing; off by default, because a relation that
   *        already holds only what is drawn gains nothing but the cost of the test
   */
  public record Query(
      @JsonProperty("minzoom") Integer minzoom,
      @JsonProperty("maxzoom") Integer maxzoom,
      @JsonProperty("from") String from,
      @JsonProperty("filter") Expression filter,
      @JsonProperty("where") String where,
      @JsonProperty("simplify") Double simplify,
      @JsonProperty("drawable") Boolean drawable) {
  }
}
