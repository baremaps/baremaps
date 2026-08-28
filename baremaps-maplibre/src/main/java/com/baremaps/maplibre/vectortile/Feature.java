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
import java.util.Map;
import org.locationtech.jts.geom.Geometry;

/**
 * A vector tile feature.
 */
public class Feature {

  private long id;

  private Map<String, Object> tags;

  private Geometry geometry;

  /**
   * Creates a new feature.
   */
  public Feature() {}

  /**
   * Creates a new feature.
   *
   * @param id The id of the feature.
   * @param tags The tags of the feature.
   * @param geometry The geometry of the feature.
   */
  public Feature(long id, Map<String, Object> tags, Geometry geometry) {
    this.id = id;
    this.tags = tags;
    this.geometry = geometry;
  }

  /**
   * Returns the id of the feature.
   *
   * @return The id of the feature.
   */
  public long getId() {
    return id;
  }

  /**
   * Sets the id of the feature.
   *
   * @param id The id of the feature.
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Returns the tags of the feature.
   *
   * @return The tags of the feature.
   */
  public Map<String, Object> getTags() {
    return tags;
  }

  /**
   * Sets the tags of the feature.
   *
   * @param tags The tags of the feature.
   */
  public void setTags(Map<String, Object> tags) {
    this.tags = tags;
  }

  /**
   * Returns the geometry of the feature.
   *
   * @return The geometry of the feature.
   */
  public Geometry getGeometry() {
    return geometry;
  }

  /**
   * Sets the geometry of the feature.
   *
   * @param geometry The geometry of the feature.
   */
  public void setGeometry(Geometry geometry) {
    this.geometry = geometry;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Feature feature = (Feature) o;
    return id == feature.id
        && Objects.equal(tags, feature.tags)
        && Objects.equal(geometry, feature.geometry);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, tags, geometry);
  }
}
