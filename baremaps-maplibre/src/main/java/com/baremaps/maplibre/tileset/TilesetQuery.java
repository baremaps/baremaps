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

public class TilesetQuery {

  @JsonProperty("minzoom")
  private Integer minzoom;

  @JsonProperty("maxzoom")
  private Integer maxzoom;

  @JsonProperty("sql")
  private String sql;

  public TilesetQuery() {}

  public TilesetQuery(Integer minzoom, Integer maxzoom, String sql) {
    this.minzoom = minzoom;
    this.maxzoom = maxzoom;
    this.sql = sql;
  }

  public Integer getMinzoom() {
    return minzoom;
  }

  public TilesetQuery setMinzoom(Integer minzoom) {
    this.minzoom = minzoom;
    return this;
  }

  public Integer getMaxzoom() {
    return maxzoom;
  }

  public TilesetQuery setMaxzoom(Integer maxzoom) {
    this.maxzoom = maxzoom;
    return this;
  }

  public String getSql() {
    return sql;
  }

  public TilesetQuery setSql(String sql) {
    this.sql = sql;
    return this;
  }
}
