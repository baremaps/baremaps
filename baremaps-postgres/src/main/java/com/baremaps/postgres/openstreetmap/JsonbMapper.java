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

/** Converts between the tags of an entity and the {@code jsonb} column that holds them. */
class JsonbMapper {

  private static final ObjectMapper mapper = new ObjectMapper();

  private static final TypeReference<HashMap<String, Object>> TAGS = new TypeReference<>() {};

  private JsonbMapper() {}

  /**
   * Serializes the tags of an entity.
   *
   * @param tags the tags, indexed by key
   * @return the JSON object holding them
   */
  public static String toJson(Map<String, Object> tags) throws JsonProcessingException {
    return mapper.writeValueAsString(tags);
  }

  /**
   * Deserializes the tags of an entity.
   *
   * @param json the JSON object read from the column
   * @return the tags, indexed by key
   */
  public static Map<String, Object> toMap(String json) throws JsonProcessingException {
    return mapper.readValue(json, TAGS);
  }
}
