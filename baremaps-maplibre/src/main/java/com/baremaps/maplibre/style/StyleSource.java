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

package com.baremaps.maplibre.style;



import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * A source is a collection of data that is used as a single input to style layers.
 *
 * @see <a href=
 *      "https://maplibre.org/maplibre-style-spec/sources/">https://maplibre.org/maplibre-style-spec/sources/</a>
 */
public class StyleSource {

  @JsonProperty("type")
  private String type;

  @JsonProperty("attribution")
  private String attribution;

  @JsonProperty("bounds")
  private List<Double> bounds;

  @JsonProperty("minzoom")
  private Integer minzoom;

  @JsonProperty("maxzoom")
  private Integer maxzoom;

  @JsonProperty("promoteId")
  private String promoteId;

  @JsonProperty("scheme")
  private String scheme;

  @JsonProperty("tiles")
  private List<String> tiles;

  @JsonProperty("url")
  private String url;

  @JsonProperty("volatile")
  private Boolean isVolatile;

  @JsonProperty("tileSize")
  private Integer tileSize;

  @JsonProperty("encoding")
  private String encoding;

  /**
   * Constructs a style source.
   */
  public StyleSource() {}

  /**
   * Constructs a style source.
   *
   * @param type the type of the source
   * @param url the url of the source
   */
  public StyleSource(String type, String url) {
    this.type = type;
    this.url = url;
  }

  /**
   * Returns the type of the source.
   *
   * @return the type of the source
   */
  public String getType() {
    return type;
  }

  /**
   * Returns the url of the source.
   *
   * @return the url of the source
   */
  public StyleSource setType(String type) {
    this.type = type;
    return this;
  }

  /**
   * Returns the url of the source.
   *
   * @return the url of the source
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the url of the source.
   *
   * @param url the url of the source
   * @return the source
   */
  public StyleSource setUrl(String url) {
    this.url = url;
    return this;
  }

  /**
   * Returns the tiles of the source.
   *
   * @return the tiles of the source
   */
  public String getAttribution() {
    return attribution;
  }

  /**
   * Sets the attribution of the source.
   *
   * @param attribution the attribution of the source
   * @return the source
   */
  public StyleSource setAttribution(String attribution) {
    this.attribution = attribution;
    return this;
  }

  /**
   * Returns the bounds of the source.
   *
   * @return the bounds of the source
   */
  public List<Double> getBounds() {
    return bounds;
  }

  /**
   * Sets the bounds of the source.
   *
   * @param bounds the bounds of the source
   * @return the source
   */
  public StyleSource setBounds(List<Double> bounds) {
    this.bounds = bounds;
    return this;
  }

  /**
   * Returns the minzoom of the source.
   *
   * @return the minzoom of the source
   */
  public Integer getMinzoom() {
    return minzoom;
  }

  /**
   * Sets the minzoom of the source.
   *
   * @param minzoom the minzoom of the source
   * @return the source
   */
  public StyleSource setMinzoom(Integer minzoom) {
    this.minzoom = minzoom;
    return this;
  }

  /**
   * Returns the maxzoom of the source.
   *
   * @return the maxzoom of the source
   */
  public Integer getMaxzoom() {
    return maxzoom;
  }

  /**
   * Sets the maxzoom of the source.
   *
   * @param maxzoom the maxzoom of the source
   * @return the source
   */
  public StyleSource setMaxzoom(Integer maxzoom) {
    this.maxzoom = maxzoom;
    return this;
  }

  /**
   * Returns the promoteId of the source.
   *
   * @return the promoteId of the source
   */
  public String getPromoteId() {
    return promoteId;
  }

  /**
   * Sets the promoteId of the source.
   *
   * @param promoteId the promoteId of the source
   * @return the source
   */
  public StyleSource setPromoteId(String promoteId) {
    this.promoteId = promoteId;
    return this;
  }

  /**
   * Returns the scheme of the source.
   *
   * @return the scheme of the source
   */
  public String getScheme() {
    return scheme;
  }

  /**
   * Sets the scheme of the source.
   *
   * @param scheme the scheme of the source
   * @return the source
   */
  public StyleSource setScheme(String scheme) {
    this.scheme = scheme;
    return this;
  }

  /**
   * Returns the tiles of the source.
   *
   * @return the tiles of the source
   */
  public List<String> getTiles() {
    return tiles;
  }

  /**
   * Sets the tiles of the source.
   *
   * @param tiles the tiles of the source
   * @return the source
   */
  public StyleSource setTiles(List<String> tiles) {
    this.tiles = tiles;
    return this;
  }

  /**
   * Returns the volatile of the source.
   *
   * @return the volatile of the source
   */
  public Boolean getVolatile() {
    return isVolatile;
  }

  /**
   * Sets the volatile of the source.
   *
   * @param isVolatile the volatile of the source
   * @return the source
   */
  public StyleSource setVolatile(Boolean isVolatile) {
    this.isVolatile = isVolatile;
    return this;
  }

  /**
   * Returns the tileSize of the source.
   *
   * @return the tileSize of the source
   */
  public Integer getTileSize() {
    return tileSize;
  }

  /**
   * Sets the tileSize of the source.
   *
   * @param tileSize the tileSize of the source
   * @return the source
   */
  public StyleSource setTileSize(Integer tileSize) {
    this.tileSize = tileSize;
    return this;
  }

  /**
   * Returns the encoding of the source.
   *
   * @return the encoding of the source
   */
  public String getEncoding() {
    return encoding;
  }

  /**
   * Sets the encoding of the source.
   *
   * @param encoding the encoding of the source
   * @return the source
   */
  public StyleSource setEncoding(String encoding) {
    this.encoding = encoding;
    return this;
  }
}
