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

/** Represents a node element in an OpenStreetMap dataset. */
public final class Node extends Element {

  private Double lon;

  private Double lat;

  /**
   * Constructs an OpenStreetMap {@code Node}.
   */
  public Node() {
    super();
  }

  /**
   * Constructs an OpenStreetMap {@code Node} with the specified parameters.
   *
   * @param id the id
   * @param info the information
   * @param tags the tags
   * @param lon the longitude
   * @param lat the latitude
   */
  public Node(Long id, Info info, Map<String, Object> tags, Double lon, Double lat) {
    super(id, info, tags);
    this.lon = lon;
    this.lat = lat;
  }

  /**
   * Constructs an OpenStreetMap {@code Node} with the specified parameters.
   *
   * @param id the id
   * @param info the information
   * @param tags the tags
   * @param lon the longitude
   * @param lat the latitude
   * @param geometry the geometry
   */
  public Node(Long id, Info info, Map<String, Object> tags, Double lon, Double lat,
      Geometry geometry) {
    super(id, info, tags, geometry);
    this.lon = lon;
    this.lat = lat;
  }

  /**
   * Returns the longitude.
   *
   * @return the longitude
   */
  public Double getLon() {
    return lon;
  }

  /**
   * Sets the longitude.
   *
   * @param lon the longitude
   */
  public void setLon(Double lon) {
    this.lon = lon;
  }

  /**
   * Returns the latitude.
   *
   * @return the latitude
   */
  public Double getLat() {
    return lat;
  }

  /**
   * Sets the latitude.
   *
   * @param lat the latitude
   */
  public void setLat(Double lat) {
    this.lat = lat;
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Node)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    Node node = (Node) o;
    return Double.compare(node.lon, lon) == 0 && Double.compare(node.lat, lat) == 0;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), lon, lat);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return new StringJoiner(", ", Node.class.getSimpleName() + "[", "]").add("lon=" + lon)
        .add("lat=" + lat).add("id=" + id).toString();
  }
}
