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


import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * A layer of a map: how it is drawn, and, for one layer per source layer, where its features come
 * from.
 *
 * <p>
 * The members are named the way the rest of the map document names things, so a layer reads without
 * quotes around half its keys. The style specification writes the source layer as
 * {@code source-layer}, and {@link MapCompiler} restores that spelling on the way out; the property
 * names inside {@code layout} and {@code paint} are passed through untouched, because those belong
 * to the specification rather than to this format.
 *
 * <p>
 * A source layer usually feeds several layers: nine draw from {@code highway}, seven from
 * {@code point}. Repeating the queries on each of them would be repeating something that has one
 * answer, so exactly one layer declares them and {@link MapCompiler} rejects a source layer that is
 * declared twice or not at all.
 */
public class MapLayer {

  @JsonProperty("id")
  private String id;

  @JsonProperty("type")
  private String type;

  @JsonProperty("filter")
  private List<Object> filter;

  @JsonProperty("source")
  private String source;

  @JsonProperty("sourceLayer")
  private String sourceLayer;

  @JsonProperty("sourceQueries")
  private List<MapSpec.Query> sourceQueries;

  @JsonProperty("generalize")
  private MapSpec.Generalize generalize;

  @JsonProperty("layout")
  private Object layout;

  @JsonProperty("minzoom")
  private Integer minzoom;

  @JsonProperty("maxzoom")
  private Integer maxzoom;

  @JsonProperty("paint")
  private Object paint;

  /**
   * Rejects the hyphenated spellings the style specification uses.
   *
   * <p>
   * A layer pasted from a style would otherwise carry {@code source-layer}, which nothing here
   * reads. Ignoring it would leave the layer with no source layer and nothing to draw, and it would
   * draw nothing quietly, so it is refused instead. Anything else unrecognised is kept, so that a
   * specification written against a later version of the style specification still loads.
   */
  @JsonAnySetter
  public void unknown(String name, Object value) {
    var camel = Map.of(
        "source-layer", "sourceLayer",
        "source-queries", "sourceQueries");
    if (camel.containsKey(name)) {
      throw new IllegalArgumentException(String.format(
          "Layer '%s' uses '%s'; this format writes it as '%s'.",
          id == null ? "?" : id, name, camel.get(name)));
    }
  }

  public String getId() {
    return id;
  }

  public MapLayer setId(String id) {
    this.id = id;
    return this;
  }

  public String getType() {
    return type;
  }

  public MapLayer setType(String type) {
    this.type = type;
    return this;
  }

  public List<Object> getFilter() {
    return filter;
  }

  public MapLayer setFilter(List<Object> filter) {
    this.filter = filter;
    return this;
  }

  public String getSource() {
    return source;
  }

  public MapLayer setSource(String source) {
    this.source = source;
    return this;
  }

  public String getSourceLayer() {
    return sourceLayer;
  }

  public MapLayer setSourceLayer(String sourceLayer) {
    this.sourceLayer = sourceLayer;
    return this;
  }

  public List<MapSpec.Query> getSourceQueries() {
    return sourceQueries;
  }

  public MapLayer setSourceQueries(List<MapSpec.Query> sourceQueries) {
    this.sourceQueries = sourceQueries;
    return this;
  }

  public MapSpec.Generalize getGeneralize() {
    return generalize;
  }

  public MapLayer setGeneralize(MapSpec.Generalize generalize) {
    this.generalize = generalize;
    return this;
  }

  public Object getLayout() {
    return layout;
  }

  public MapLayer setLayout(Object layout) {
    this.layout = layout;
    return this;
  }

  public Integer getMinzoom() {
    return minzoom;
  }

  public MapLayer setMinzoom(Integer minzoom) {
    this.minzoom = minzoom;
    return this;
  }

  public Integer getMaxzoom() {
    return maxzoom;
  }

  public MapLayer setMaxzoom(Integer maxzoom) {
    this.maxzoom = maxzoom;
    return this;
  }

  public Object getPaint() {
    return paint;
  }

  public MapLayer setPaint(Object paint) {
    this.paint = paint;
    return this;
  }
}
