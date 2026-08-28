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

package com.baremaps.maplibre.vectortile;

import com.google.common.base.Objects;
import java.util.List;

/**
 * A vector tile layer.
 */
public class Tile {

  private List<Layer> layers;

  /**
   * Creates a new tile.
   */
  public Tile(List<Layer> layers) {
    this.layers = layers;
  }

  /**
   * Returns the layers of the tile.
   *
   * @return The layers of the tile.
   */
  public List<Layer> getLayers() {
    return layers;
  }

  /**
   * Sets the layers of the tile.
   *
   * @param layers The layers of the tile.
   */
  public void setLayers(List<Layer> layers) {
    this.layers = layers;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Tile tile = (Tile) o;
    return Objects.equal(layers, tile.layers);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(layers);
  }
}
