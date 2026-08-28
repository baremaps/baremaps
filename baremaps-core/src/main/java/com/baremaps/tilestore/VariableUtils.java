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

package com.baremaps.tilestore;



import java.util.Map;
import java.util.Map.Entry;

/** A utility class to interpolate variables in strings. */
public class VariableUtils {

  private VariableUtils() {}

  /**
   * Interpolates variables prefixed with '$' in a template string.
   *
   * @param variables the variables
   * @param template the template
   * @return the result
   */
  public static String interpolate(Map<String, String> variables, String template) {
    for (Entry<String, String> entry : variables.entrySet()) {
      template = template.replace(String.format("$%s", entry.getKey()), entry.getValue());
    }
    return template;
  }
}
