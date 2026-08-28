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
public class Layer {

  private String name;

  private int extent;

  private List<Feature> features;

  /**
   * Creates a new layer.
   */
  public Layer() {

  }

  /**
   * Creates a new layer.
   *
   * @param name The name of the layer.
   * @param extent The extent of the layer.
   * @param features The features of the layer.
   */
  public Layer(String name, int extent, List<Feature> features) {
    this.name = name;
    this.extent = extent;
    this.features = features;
  }

  /**
   * Returns the name of the layer.
   *
   * @return The name of the layer.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the layer.
   *
   * @param name The name of the layer.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the extent of the layer.
   *
   * @return The extent of the layer.
   */
  public int getExtent() {
    return extent;
  }

  /**
   * Sets the extent of the layer.
   *
   * @param extent The extent of the layer.
   */
  public void setExtent(int extent) {
    this.extent = extent;
  }

  /**
   * Returns the features of the layer.
   *
   * @return The features of the layer.
   */
  public List<Feature> getFeatures() {
    return features;
  }

  /**
   * Sets the features of the layer.
   *
   * @param features The features of the layer.
   */
  public void setFeatures(List<Feature> features) {
    this.features = features;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Layer layer = (Layer) o;
    return extent == layer.extent
        && Objects.equal(name, layer.name)
        && Objects.equal(features, layer.features);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, extent, features);
  }
}
