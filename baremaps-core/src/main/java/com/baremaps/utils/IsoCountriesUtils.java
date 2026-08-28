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

package com.baremaps.utils;



import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class to deal with country codes.
 */
public class IsoCountriesUtils {

  private static final Map<String, String> ISO_COUNTRIES = new HashMap<>();

  static {
    for (String iso : Locale.getISOCountries()) {
      Locale l = new Locale("", iso);
      ISO_COUNTRIES.put(iso, l.getDisplayCountry(Locale.ENGLISH));
    }
  }

  private IsoCountriesUtils() {}

  public static String getCountry(String iso) {
    return ISO_COUNTRIES.getOrDefault(iso, "");
  }

  public static boolean containsCountry(String iso) {
    return ISO_COUNTRIES.containsKey(iso);
  }
}
