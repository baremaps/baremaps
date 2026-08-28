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

package com.baremaps.postgres.openstreetmap;



import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

class JsonbMapper {

  private static final ObjectMapper mapper = new ObjectMapper();

  private JsonbMapper() {}

  /**
   * Convert a map into a json object
   *
   * @param input a map with the entry of the object
   * @return a Json string representing the object
   * @throws JsonProcessingException
   */
  public static String toJson(Map<String, Object> input) throws JsonProcessingException {
    return mapper.writeValueAsString(input);
  }

  /**
   * Convert a Json array into a map
   *
   * @param input a valid json object
   * @return a map with the entry of the objects
   * @throws JsonProcessingException
   */
  public static Map<String, Object> toMap(String input) throws JsonProcessingException {
    TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {};
    return mapper.readValue(input, typeRef);
  }
}
