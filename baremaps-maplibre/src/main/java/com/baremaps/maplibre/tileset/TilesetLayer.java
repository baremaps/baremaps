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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TilesetLayer {

  @JsonProperty("id")
  private String id;

  @JsonProperty("fields")
  private Map<String, String> fields = new HashMap<>();

  @JsonProperty("description")
  private String description;

  @JsonProperty("minzoom")
  private Integer minzoom;

  @JsonProperty("maxzoom")
  private Integer maxzoom;

  @JsonProperty("queries")
  private List<TilesetQuery> queries = new ArrayList<>();

  public TilesetLayer() {}

  public TilesetLayer(String id, Map<String, String> fields, String description, Integer minzoom,
      Integer maxzoom, List<TilesetQuery> queries) {
    this.id = id;
    this.fields = fields;
    this.description = description;
    this.minzoom = minzoom;
    this.maxzoom = maxzoom;
    this.queries = queries;
  }

  public String getId() {
    return id;
  }

  public TilesetLayer setId(String id) {
    this.id = id;
    return this;
  }

  public Map<String, String> getFields() {
    return fields;
  }

  public TilesetLayer setFields(Map<String, String> fields) {
    this.fields = fields;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public TilesetLayer setDescription(String description) {
    this.description = description;
    return this;
  }

  public Integer getMinzoom() {
    return minzoom;
  }

  public TilesetLayer setMinzoom(Integer minzoom) {
    this.minzoom = minzoom;
    return this;
  }

  public Integer getMaxzoom() {
    return maxzoom;
  }

  public TilesetLayer setMaxzoom(Integer maxzoom) {
    this.maxzoom = maxzoom;
    return this;
  }

  public List<TilesetQuery> getQueries() {
    return queries;
  }

  public TilesetLayer setQueries(List<TilesetQuery> queries) {
    this.queries = queries;
    return this;
  }
}
