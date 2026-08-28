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
import java.util.ArrayList;
import java.util.List;

/**
 * A layer is a group of rendering instructions that are applied to a source.
 *
 * @see <a href=
 *      "https://docs.mapbox.com/mapbox-gl-js/style-spec/layers/">https://docs.mapbox.com/mapbox-gl-js/style-spec/layers/</a>
 */
public class StyleLayer {

  @JsonProperty("id")
  private String id;

  @JsonProperty("type")
  private String type;

  @JsonProperty("filter")
  private List<Object> filter = new ArrayList<Object>();

  @JsonProperty("source")
  private String source;

  @JsonProperty("source-layer")
  private String sourceLayer;

  @JsonProperty("layout")
  private Object layout;

  @JsonProperty("minzoom")
  private Integer minzoom;

  @JsonProperty("maxzoom")
  private Integer maxzoom;

  @JsonProperty("paint")
  private Object paint;

  /**
   * Constructs a style layer.
   */
  public StyleLayer() {}

  /**
   * Constructs a style layer.
   *
   * @param type the type of the layer
   * @param filter the filter of the layer
   * @param source the source of the layer
   * @param sourceLayer the source layer of the layer
   * @param layout the layout of the layer
   * @param minzoom the minimum zoom of the layer
   * @param maxzoom the maximum zoom of the layer
   * @param paint the paint of the layer
   */
  @SuppressWarnings("squid:S107")
  public StyleLayer(
      String type,
      List<Object> filter,
      String source,
      String sourceLayer,
      Object layout,
      Integer minzoom,
      Integer maxzoom,
      Object paint) {
    this.type = type;
    this.filter = filter;
    this.source = source;
    this.sourceLayer = sourceLayer;
    this.layout = layout;
    this.minzoom = minzoom;
    this.maxzoom = maxzoom;
    this.paint = paint;
  }

  /**
   * Returns the id of the layer.
   *
   * @return the id of the layer
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the id of the layer.
   *
   * @param id the id of the layer
   * @return the layer
   */
  public StyleLayer setId(String id) {
    this.id = id;
    return this;
  }

  /**
   * Returns the type of the layer.
   *
   * @return the type of the layer
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type of the layer.
   *
   * @param type the type of the layer
   * @return the layer
   */
  public StyleLayer setType(String type) {
    this.type = type;
    return this;
  }

  /**
   * Returns the filter of the layer.
   *
   * @return the filter of the layer
   */
  public List<Object> getFilter() {
    return filter;
  }

  /**
   * Sets the filter of the layer.
   *
   * @param filter the filter of the layer
   * @return the layer
   */
  public StyleLayer setFilter(List<Object> filter) {
    this.filter = filter;
    return this;
  }

  /**
   * Returns the source of the layer.
   *
   * @return the source of the layer
   */
  public String getSource() {
    return source;
  }

  /**
   * Sets the source of the layer.
   *
   * @param source the source of the layer
   * @return the layer
   */
  public StyleLayer setSource(String source) {
    this.source = source;
    return this;
  }

  /**
   * Returns the source layer of the layer.
   *
   * @return the source layer of the layer
   */
  public String getSourceLayer() {
    return sourceLayer;
  }

  /**
   * Sets the source layer of the layer.
   *
   * @param sourceLayer the source layer of the layer
   * @return the layer
   */
  public StyleLayer setSourceLayer(String sourceLayer) {
    this.sourceLayer = sourceLayer;
    return this;
  }

  /**
   * Returns the layout of the layer.
   *
   * @return the layout of the layer
   */
  public Object getLayout() {
    return layout;
  }

  /**
   * Sets the layout of the layer.
   *
   * @param layout the layout of the layer
   * @return the layer
   */
  public StyleLayer setLayout(Object layout) {
    this.layout = layout;
    return this;
  }

  /**
   * Returns the minimum zoom of the layer.
   *
   * @return the minimum zoom of the layer
   */
  public Integer getMinzoom() {
    return minzoom;
  }

  /**
   * Sets the minimum zoom of the layer.
   *
   * @param minzoom the minimum zoom of the layer
   * @return the layer
   */
  public StyleLayer setMinzoom(Integer minzoom) {
    this.minzoom = minzoom;
    return this;
  }

  /**
   * Returns the maximum zoom of the layer.
   *
   * @return the maximum zoom of the layer
   */
  public Integer getMaxzoom() {
    return maxzoom;
  }

  /**
   * Sets the maximum zoom of the layer.
   *
   * @param maxzoom the maximum zoom of the layer
   * @return the layer
   */
  public StyleLayer setMaxzoom(Integer maxzoom) {
    this.maxzoom = maxzoom;
    return this;
  }

  /**
   * Returns the paint of the layer.
   *
   * @return the paint of the layer
   */
  public Object getPaint() {
    return paint;
  }

  /**
   * Sets the paint of the layer.
   *
   * @param paint the paint of the layer
   * @return the layer
   */
  public StyleLayer setPaint(Object paint) {
    this.paint = paint;
    return this;
  }
}
