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

package com.baremaps.shapefile;



import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a ShapefileType Enumeration
 *
 * <p>
 * <div class="warning">This is an experimental class, not yet target for any Apache SIS release at
 * this time.</div>
 *
 * @author Travis L. Pinney
 * @see <a href="http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf">ESRI Shapefile
 *      Specification</a>
 */
public enum ShapefileGeometryType {
  NULL_SHAPE(0),
  POINT(1),
  POLY_LINE(3),
  POLYGON(5),
  MULTI_POINT(8),
  POINT_Z(11),
  POLY_LINE_Z(13),
  POLYGON_Z(15),
  MULTI_POINT_Z(18),
  POINT_M(21),
  POLY_LINE_M(23),
  POLYGON_M(25),
  MULTI_POINT_M(28),
  MULTI_PATCH(31);

  // used for initializing the enumeration
  private int value;

  private ShapefileGeometryType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  private static final Map<Integer, ShapefileGeometryType> lookup =
      new HashMap<Integer, ShapefileGeometryType>();

  static {
    for (ShapefileGeometryType ste : EnumSet.allOf(ShapefileGeometryType.class)) {
      lookup.put(ste.getValue(), ste);
    }
  }

  public static ShapefileGeometryType get(int value) {
    return lookup.get(value);
  }
}
