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

package com.baremaps.maplibre.tileset;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * A loose implementation of the Mapbox TileJSON specification. Extended with custom fields. Must be
 * used inside baremaps componenent and not exposed to browser client.
 *
 * @see <a href=
 *      "https://docs.mapbox.com/help/glossary/tilejson/">https://docs.mapbox.com/help/glossary/tilejson/</a>
 */
public class Tileset {

  @JsonProperty("tilejson")
  private String tilejson = "2.2.0";

  @JsonProperty("name")
  private String name;

  @JsonProperty("description")
  private String description;

  @JsonProperty("version")
  private String version;

  @JsonProperty("attribution")
  private String attribution;

  @JsonProperty("template")
  private String template;

  @JsonProperty("legend")
  private String legend;

  @JsonProperty("scheme")
  private String scheme;

  @JsonProperty("tiles")
  private List<String> tiles = new ArrayList<>();

  @JsonProperty("grids")
  private List<String> grids = new ArrayList<>();

  @JsonProperty("data")
  private List<String> data = new ArrayList<>();

  @JsonProperty("minzoom")
  private Integer minzoom;

  @JsonProperty("maxzoom")
  private Integer maxzoom;

  @JsonProperty("bounds")
  private List<Double> bounds = new ArrayList<>();

  @JsonProperty("center")
  private List<Double> center = new ArrayList<>();

  @JsonProperty("database")
  private Object database;

  @JsonProperty("vector_layers")
  private List<TilesetLayer> vectorLayers = new ArrayList<>();

  private Integer fillzoom;

  public Tileset() {}

  @SuppressWarnings("squid:S107")
  public Tileset(
      String tilejson,
      String name,
      String description,
      String version,
      String attribution,
      String template,
      String legend,
      String scheme,
      List<String> tiles,
      List<String> grids,
      List<String> data,
      Integer minzoom,
      Integer maxzoom,
      List<Double> bounds,
      List<Double> center,
      List<TilesetLayer> vectorLayers,
      Integer fillzoom) {
    this.tilejson = tilejson;
    this.name = name;
    this.description = description;
    this.version = version;
    this.attribution = attribution;
    this.template = template;
    this.legend = legend;
    this.scheme = scheme;
    this.tiles = tiles;
    this.grids = grids;
    this.data = data;
    this.minzoom = minzoom;
    this.maxzoom = maxzoom;
    this.bounds = bounds;
    this.center = center;
    this.vectorLayers = vectorLayers;
    this.fillzoom = fillzoom;
  }

  public String getTilejson() {
    return tilejson;
  }

  public Tileset setTilejson(String tilejson) {
    this.tilejson = tilejson;
    return this;
  }

  public String getName() {
    return name;
  }

  public Tileset setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Tileset setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public Tileset setVersion(String version) {
    this.version = version;
    return this;
  }

  public String getAttribution() {
    return attribution;
  }

  public Tileset setAttribution(String attribution) {
    this.attribution = attribution;
    return this;
  }

  public String getTemplate() {
    return template;
  }

  public Tileset setTemplate(String template) {
    this.template = template;
    return this;
  }

  public String getLegend() {
    return legend;
  }

  public Tileset setLegend(String legend) {
    this.legend = legend;
    return this;
  }

  public String getScheme() {
    return scheme;
  }

  public Tileset setScheme(String scheme) {
    this.scheme = scheme;
    return this;
  }

  public List<String> getTiles() {
    return tiles;
  }

  public Tileset setTiles(List<String> tiles) {
    this.tiles = tiles;
    return this;
  }

  public List<String> getGrids() {
    return grids;
  }

  public Tileset setGrids(List<String> grids) {
    this.grids = grids;
    return this;
  }

  public List<String> getData() {
    return data;
  }

  public Tileset setData(List<String> data) {
    this.data = data;
    return this;
  }

  public Integer getMinzoom() {
    return minzoom;
  }

  public Tileset setMinzoom(Integer minzoom) {
    this.minzoom = minzoom;
    return this;
  }

  public Integer getMaxzoom() {
    return maxzoom;
  }

  public Tileset setMaxzoom(Integer maxzoom) {
    this.maxzoom = maxzoom;
    return this;
  }

  public List<Double> getBounds() {
    return bounds;
  }

  public Tileset setBounds(List<Double> bounds) {
    this.bounds = bounds;
    return this;
  }

  public List<Double> getCenter() {
    return center;
  }

  public Tileset setCenter(List<Double> center) {
    this.center = center;
    return this;
  }

  public Object getDatabase() {
    return database;
  }

  public void setDatabase(Object database) {
    this.database = database;
  }

  public List<TilesetLayer> getVectorLayers() {
    return vectorLayers;
  }

  public Tileset setVectorLayers(List<TilesetLayer> vectorLayers) {
    this.vectorLayers = vectorLayers;
    return this;
  }

  public Integer getFillzoom() {
    return fillzoom;
  }

  public Tileset setFillzoom(Integer fillzoom) {
    this.fillzoom = fillzoom;
    return this;
  }
}
