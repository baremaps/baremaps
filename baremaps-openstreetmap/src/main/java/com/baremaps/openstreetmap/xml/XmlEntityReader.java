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

package com.baremaps.openstreetmap.xml;

import static com.baremaps.openstreetmap.stream.ConsumerUtils.consumeThenReturn;

import com.baremaps.openstreetmap.EntityReader;
import com.baremaps.openstreetmap.GeometryOptions;
import com.baremaps.openstreetmap.model.Entity;
import java.io.InputStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Reads an OpenStreetMap XML file as a stream of entities. */
public class XmlEntityReader implements EntityReader<Entity> {

  private final GeometryOptions geometryOptions;

  /** Creates a reader that leaves geometries unset. */
  public XmlEntityReader() {
    this(null);
  }

  /**
   * Creates a reader that sets the geometry of the elements it reads.
   *
   * @param geometryOptions the geometry options, or null to leave geometries unset
   */
  public XmlEntityReader(GeometryOptions geometryOptions) {
    this.geometryOptions = geometryOptions;
  }

  @Override
  public Stream<Entity> read(InputStream input) {
    var entities = StreamSupport.stream(new XmlEntitySpliterator(input), false);
    if (geometryOptions == null) {
      return entities;
    }
    return entities.map(consumeThenReturn(geometryOptions.entityHandler()));
  }
}
