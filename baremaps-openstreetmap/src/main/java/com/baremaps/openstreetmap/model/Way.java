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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.locationtech.jts.geom.Geometry;

/** Represents a way element in an OpenStreetMap dataset. */
public final class Way extends Element {

  private final List<Long> nodes;

  public Way(long id, Info info, Map<String, Object> tags, List<Long> nodes) {
    this(id, info, tags, nodes, null);
  }

  public Way(long id, Info info, Map<String, Object> tags, List<Long> nodes, Geometry geometry) {
    super(id, info, tags, geometry);
    this.nodes = nodes;
  }

  /** Returns the ids of the nodes of the way, in order. */
  public List<Long> getNodes() {
    return nodes;
  }

  @Override
  public Way withTags(Map<String, Object> tags) {
    return new Way(getId(), getInfo(), tags, nodes, getGeometry());
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Way way && super.equals(o) && Objects.equals(nodes, way.nodes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), nodes);
  }
}
