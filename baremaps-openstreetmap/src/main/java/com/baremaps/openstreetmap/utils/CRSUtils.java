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

package com.baremaps.openstreetmap.utils;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

/**
 * Utility methods for creating coordinate reference systems.
 * <p>
 * This method first looks into a minimal set of crs definitions that are hardcoded in the
 * application. This includes the "WSG 84" and "WGS 84 / Pseudo-Mercator" coordinate reference
 * systems.
 * <p>
 * Then, it tries to create a CRS from the EPSG code that may be stored in the proj4/nad/epsg
 * resource file of the proj4j-epsg module.
 * <p>
 * The proj4j-epsg module is not included in baremaps by default due to licensing issues. It can be
 * added to the classpath to enable the creation of CRS from EPSG codes.
 */
public class CRSUtils {

  private CRSUtils() {
    // Prevent instantiation
  }

  private static final CRSFactory CRS_FACTORY = new CRSFactory();

  private static final CoordinateReferenceSystem WGS_84 =
      CRS_FACTORY.createFromParameters("WGS 84", new String[] {
          "+proj=longlat",
          "+datum=WGS84",
          "+no_defs"
      });

  private static final CoordinateReferenceSystem WGS_84_PSEUDO_MERCATOR =
      CRS_FACTORY.createFromParameters("WGS 84 / Pseudo-Mercator", new String[] {
          "+proj=merc",
          "+a=6378137",
          "+b=6378137",
          "+lat_ts=0.0",
          "+lon_0=0.0",
          "+x_0=0.0",
          "+y_0=0",
          "+k=1.0",
          "+units=m",
          "+nadgrids=@null",
          "+wktext",
          "+no_defs"
      });

  /**
   * Creates a coordinate reference system from the provided SRID.
   *
   * @param srid the SRID
   * @return the coordinate reference system
   */
  public static CoordinateReferenceSystem createFromSrid(int srid) {
    switch (srid) {
      case 4326:
        return WGS_84;
      case 3857:
        return WGS_84_PSEUDO_MERCATOR;
      default:
        return CRS_FACTORY.createFromName(String.format("EPSG:%s", srid));
    }
  }
}
