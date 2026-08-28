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
 *
 * <p>
 * The identity of an element (id, info, tags, members) is fixed at construction. The geometry is
 * the one deliberately mutable slot: it is derived from other elements and is set by the geometry
 * builders once the coordinates they depend on have been streamed.
 */
public abstract sealed

class Element implements Entity
permits Node, Way, Relation
{

  private final long id;
  private final Info info;
  private final Map<String, Object> tags;
  private Geometry geometry;

  protected Element(long id, Info info, Map<String, Object> tags, Geometry geometry) {
    this.id = id;
    this.info = info;
    this.tags = tags;
    this.geometry = geometry;
  }

  public long getId() {
    return id;
  }

  public Info getInfo() {
    return info;
  }

  public Map<String, Object> getTags() {
    return tags;
  }

  /**
   * Returns a copy of this element with the provided tags.
   *
   * @param tags the tags
   * @return a copy of this element
   */
  public abstract Element withTags(Map<String, Object> tags);

  public Geometry getGeometry() {
    return geometry;
  }

  public void setGeometry(Geometry geometry) {
    this.geometry = geometry;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Element element)) {
      return false;
    }
    return id == element.id && Objects.equals(info, element.info)
        && Objects.equals(tags, element.tags) && Objects.equals(geometry, element.geometry);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, info, tags, geometry);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", getClass().getSimpleName() + "[", "]").add("id=" + id)
        .add("info=" + info).add("tags=" + tags).add("geometry=" + geometry).toString();
  }
}
