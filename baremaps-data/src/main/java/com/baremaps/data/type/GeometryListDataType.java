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

package com.baremaps.data.type;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;

/**
 * The base of the {@link DataType}s for geometries made of other geometries, stored as a
 * {@link ListDataType} of their members.
 */
abstract class GeometryListDataType<G extends GeometryCollection, E extends Geometry>
    implements DataType<G> {

  private final ListDataType<E> members;

  protected GeometryListDataType(DataType<E> memberType) {
    this.members = new ListDataType<>(memberType);
  }

  /** Builds the geometry from its members. */
  protected abstract G create(List<E> members);

  @SuppressWarnings("unchecked")
  private List<E> membersOf(G value) {
    var list = new ArrayList<E>(value.getNumGeometries());
    for (int i = 0; i < value.getNumGeometries(); i++) {
      list.add((E) value.getGeometryN(i));
    }
    return list;
  }

  @Override
  public int size(final G value) {
    return members.size(membersOf(value));
  }

  @Override
  public int size(final MemorySegment segment, final long position) {
    return members.size(segment, position);
  }

  @Override
  public void write(final MemorySegment segment, final long position, final G value) {
    members.write(segment, position, membersOf(value));
  }

  @Override
  public G read(final MemorySegment segment, final long position) {
    return create(members.read(segment, position));
  }
}
