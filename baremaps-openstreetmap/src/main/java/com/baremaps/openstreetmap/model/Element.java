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

package com.baremaps.openstreetmap.model;



import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import org.locationtech.jts.geom.Geometry;

/**
 * Represents an element in an OpenStreetMap dataset. Elements are a basis to model the physical
 * world.
 */
public abstract sealed

class Element implements Entity
permits Node, Way, Relation
{

  protected Long id;

  protected Info info;

  protected Map<String, Object> tags;

  protected Geometry geometry;

  protected Element() {

  }

  protected Element(long id, Info info, Map<String, Object> tags) {
    this(id, info, tags, null);
  }

  /**
   * Constructs an OpenStreetMap {@code Element} with the specified parameters.
   *
   * @param id the id
   * @param info the {@code Info}
   * @param tags the tags
   * @param geometry the geometry
   */
  protected Element(long id, Info info, Map<String, Object> tags, Geometry geometry) {
    this.id = id;
    this.info = info;
    this.tags = tags;
    this.geometry = geometry;
  }

  /**
   * Returns the id.
   *
   * @return the id
   */
  public long getId() {
    return id;
  }

  /**
   * Sets the id.
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Returns the info.
   *
   * @return the info
   */
  public Info getInfo() {
    return info;
  }

  /**
   * Sets the info.
   *
   * @param info the info
   */
  public void setInfo(Info info) {
    this.info = info;
  }

  /**
   * Returns the tags.
   *
   * @return the tags
   */
  public Map<String, Object> getTags() {
    return tags;
  }

  /**
   * Sets the tags.
   *
   * @param tags the tags
   */
  public void setTags(Map<String, Object> tags) {
    this.tags = tags;
  }

  /**
   * Returns the geometry.
   *
   * @return the geometry
   */
  public Geometry getGeometry() {
    return this.geometry;
  }

  /**
   * Sets the geometry.
   *
   * @param geometry the geometry
   */
  public void setGeometry(Geometry geometry) {
    this.geometry = geometry;
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Element)) {
      return false;
    }
    Element element = (Element) o;
    return id == element.id && Objects.equals(info, element.info)
        && Objects.equals(tags, element.tags) && Objects.equals(geometry, element.geometry);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(id, info, tags, geometry);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return new StringJoiner(", ", Element.class.getSimpleName() + "[", "]").add("id=" + id)
        .add("info=" + info).add("tags=" + tags).add("geometry=" + geometry).toString();
  }

}
