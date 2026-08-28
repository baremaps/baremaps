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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the Mapbox Style Specification.
 *
 * @see <a href=
 *      "https://www.mapbox.com/mapbox-gl-js/style-spec/">https://www.mapbox.com/mapbox-gl-js/style-spec/</a>
 */
public class Style {

  @JsonProperty("version")
  private Integer version = 8;

  @JsonProperty("name")
  private String name;

  @JsonProperty("sources")
  private Map<String, StyleSource> sources;

  @JsonProperty("sprite")
  private String sprite;

  @JsonProperty("glyphs")
  private String glyphs;

  @JsonProperty("layers")
  private List<StyleLayer> layers;

  @JsonProperty("center")
  private List<BigDecimal> center;

  @JsonProperty("metadata")
  private Object metadata;

  @JsonProperty("zoom")
  private BigDecimal zoom;

  /**
   * Constructs a style.
   */
  public Style() {}

  /**
   * Constructs a style.
   *
   * @param version The version of the style specification to which the style conforms
   * @param name The human-readable name of the style
   * @param sources A dictionary of sources
   * @param sprite The URL of an image sprite
   * @param glyphs The URL of a glyph sprite
   * @param layers A list of layers
   * @param center The longitude and latitude (in that order) at the center of the map
   * @param metadata Arbitrary properties useful to track with the stylesheet, but do not influence
   *        rendering
   * @param zoom The zoom level of the map
   */
  @SuppressWarnings("squid:S107")
  public Style(
      Integer version,
      String name,
      Map<String, StyleSource> sources,
      String sprite,
      String glyphs,
      List<StyleLayer> layers,
      List<BigDecimal> center,
      Object metadata,
      BigDecimal zoom) {
    this.version = version;
    this.name = name;
    this.sources = sources;
    this.sprite = sprite;
    this.glyphs = glyphs;
    this.layers = layers;
    this.center = center;
    this.metadata = metadata;
    this.zoom = zoom;
  }

  /**
   * Returns the version of the style specification to which the style conforms.
   *
   * @return The version of the style specification to which the style conforms.
   */
  public Integer getVersion() {
    return version;
  }

  /**
   * Sets the version of the style specification to which the style conforms.
   *
   * @param version The version of the style specification to which the style conforms
   * @return The style
   */
  public Style setVersion(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Returns the human-readable name of the style
   *
   * @return The human-readable name of the style
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the human-readable name of the style.
   *
   * @param name The human-readable name of the style
   * @return The style
   */
  public Style setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Returns a dictionary of sources
   *
   * @return A dictionary of sources
   */
  public Map<String, StyleSource> getSources() {
    return sources;
  }

  /**
   * Sets a dictionary of sources.
   *
   * @param sources A dictionary of sources
   * @return The style
   */
  public Style setSources(Map<String, StyleSource> sources) {
    this.sources = sources;
    return this;
  }

  /**
   * Returns the URL of an image sprite.
   *
   * @return The URL of an image sprite
   */
  public String getSprite() {
    return sprite;
  }

  /**
   * Sets the URL of an image sprite.
   *
   * @param sprite The URL of an image sprite
   * @return The style
   */
  public Style setSprite(String sprite) {
    this.sprite = sprite;
    return this;
  }

  /**
   * Returns the URL of a glyph sprite.
   *
   * @return The URL of a glyph sprite
   */
  public String getGlyphs() {
    return glyphs;
  }

  /**
   * Sets the URL of a glyph sprite.
   *
   * @param glyphs The URL of a glyph sprite
   * @return The style
   */
  public Style setGlyphs(String glyphs) {
    this.glyphs = glyphs;
    return this;
  }

  /**
   * Returns a list of layers.
   *
   * @return A list of layers
   */
  public List<StyleLayer> getLayers() {
    return layers;
  }

  /**
   * Sets a list of layers.
   *
   * @param layers A list of layers
   * @return The style
   */
  public Style setLayers(List<StyleLayer> layers) {
    this.layers = layers;
    return this;
  }

  /**
   * Returns the longitude and latitude (in that order) at the center of the map.
   *
   * @return The longitude and latitude (in that order) at the center of the map
   */
  public List<BigDecimal> getCenter() {
    return center;
  }

  /**
   * Sets the longitude and latitude (in that order) at the center of the map.
   *
   * @param center The longitude and latitude (in that order) at the center of the map
   * @return The style
   */
  public Style setCenter(List<BigDecimal> center) {
    this.center = center;
    return this;
  }

  /**
   * Returns arbitrary properties useful to track with the stylesheet, but do not influence
   * rendering.
   *
   * @return Arbitrary properties useful to track with the stylesheet, but do not influence
   *         rendering
   */
  public Object getMetadata() {
    return metadata;
  }

  /**
   * Sets arbitrary properties useful to track with the stylesheet, but do not influence rendering.
   *
   * @param metadata Arbitrary properties useful to track with the stylesheet, but do not influence
   *        rendering
   * @return The style
   */
  public Style setMetadata(Object metadata) {
    this.metadata = metadata;
    return this;
  }

  /**
   * Returns the zoom level of the map.
   *
   * @return The zoom level of the map
   */
  public BigDecimal getZoom() {
    return zoom;
  }

  /**
   * Sets the zoom level of the map.
   *
   * @param zoom The zoom level of the map
   * @return The style
   */
  public Style setZoom(BigDecimal zoom) {
    this.zoom = zoom;
    return this;
  }
}
