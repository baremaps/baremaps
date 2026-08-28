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

package com.baremaps.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.baremaps.maplibre.style.Style;
import com.baremaps.testing.TestFiles;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

class ConfigReaderTest {

  @Test
  void readStyle() throws IOException {
    var config = new ConfigReader().read(TestFiles.CONFIG_STYLE_JS);
    var style = new ObjectMapper().readValue(config, Style.class);
    var source = style.getSources().get("mymap");
    assertEquals("http://my.server.com/{z}/{y}/{x}.mvt", source.getTiles().get(0));
    assertEquals(14, source.getMaxzoom());
  }
}
