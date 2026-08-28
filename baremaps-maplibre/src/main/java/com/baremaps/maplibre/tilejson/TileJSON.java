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

package com.baremaps.maplibre.tilejson;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * TileJSON is an open standard for representing map metadata. Based on version 3.3.0.
 *
 * <br>
 * Mainly used to project the `--tileset` configuration file to a version for clients.
 *
 * @see <a href="https://docs.mapbox.com/help/glossary/tilejson/">
 *      https://docs.mapbox.com/help/glossary/tilejson/</a>
 * @see <a href="https://github.com/mapbox/tilejson-spec">
 *      https://github.com/mapbox/tilejson-spec</a>
 */
public class TileJSON {

  @JsonProperty("tiles")
  List<String> tiles;

  @JsonProperty("vector_layers")
  List<VectorLayer> vectorLayers;

  @JsonProperty("attribution")
  String attribution;

  @JsonProperty("bounds")
  List<Double> bounds;

  @JsonProperty("center")
  List<Double> center;

  @JsonProperty("data")
  List<String> data;

  @JsonProperty("description")
  String description;

  @JsonProperty("fillzoom")
  Integer fillzoom;

  @JsonProperty("grids")
  List<String> grids;

  @JsonProperty("legend")
  String legend;

  @JsonProperty("maxzoom")
  Integer maxzoom;

  @JsonProperty("minzoom")
  Integer minzoom;

  @JsonProperty("name")
  String name;

  @JsonProperty("scheme")
  String scheme;

  @JsonProperty("template")
  String template;

  @JsonProperty("version")
  String version;

  public void setTiles(List<String> tiles) {
    this.tiles = tiles;
  }

  public List<VectorLayer> getVectorLayers() {
    return vectorLayers;
  }

}
