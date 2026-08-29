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

package com.baremaps.openstreetmap.pbf;

import com.baremaps.openstreetmap.EntityReader;
import com.baremaps.openstreetmap.GeometryOptions;
import com.baremaps.openstreetmap.model.Entity;
import java.io.InputStream;
import java.util.stream.Stream;

/** Reads an OpenStreetMap PBF file as a stream of entities. */
public class PbfEntityReader implements EntityReader<Entity> {

  private final PbfBlockReader blockReader;

  /** Creates a reader that leaves geometries unset. */
  public PbfEntityReader() {
    this.blockReader = new PbfBlockReader();
  }

  /**
   * Creates a reader that sets the geometry of the elements it reads.
   *
   * @param geometryOptions the geometry options
   */
  public PbfEntityReader(GeometryOptions geometryOptions) {
    this.blockReader = new PbfBlockReader(geometryOptions);
  }

  @Override
  public Stream<Entity> read(InputStream input) {
    return blockReader.read(input).flatMap(block -> block.entities().stream());
  }
}
