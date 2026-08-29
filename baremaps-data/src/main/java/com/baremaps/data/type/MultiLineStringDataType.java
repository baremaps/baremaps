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

import java.util.List;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

/** A {@link DataType} for {@link MultiLineString}s. */
public class MultiLineStringDataType extends GeometryListDataType<MultiLineString, LineString> {

  private final GeometryFactory geometryFactory;

  public MultiLineStringDataType() {
    this(new GeometryFactory());
  }

  public MultiLineStringDataType(GeometryFactory geometryFactory) {
    super(new LineStringDataType(geometryFactory));
    this.geometryFactory = geometryFactory;
  }

  @Override
  protected MultiLineString create(List<LineString> members) {
    return geometryFactory.createMultiLineString(members.toArray(LineString[]::new));
  }
}
