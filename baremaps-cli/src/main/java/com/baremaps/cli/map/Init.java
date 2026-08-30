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

package com.baremaps.cli.map;

import static com.baremaps.utils.ObjectMapperUtils.objectMapper;

import com.baremaps.maplibre.style.Style;
import com.baremaps.maplibre.style.StyleSource;
import com.baremaps.maplibre.tileset.Tileset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "init",
    description = "Init configuration files.")
public class Init implements Callable<Integer> {

  private static final Logger logger = LoggerFactory.getLogger(Init.class);

  // The files are initialized for the server started by the serve and dev commands, which is where
  // they are meant to be tried out first.
  private static final String TILEJSON_URL = "http://localhost:9000/tiles.json";

  @Option(names = {"--style"}, paramLabel = "STYLE", description = "A style file.")
  private Path style;

  @Option(names = {"--tileset"}, paramLabel = "TILESET", description = "A tileset file.")
  private Path tileset;

  @Override
  public Integer call() throws Exception {
    if (style == null && tileset == null) {
      logger.info("No configuration file specified.");
      return 0;
    }
    if (style != null) {
      write(style, style());
      logger.info("Style initialized: {}", style);
    }
    if (tileset != null) {
      write(tileset, tileset());
      logger.info("Tileset initialized: {}", tileset);
    }
    return 0;
  }

  private static Style style() {
    var source = new StyleSource();
    source.setType("vector");
    source.setUrl(TILEJSON_URL);
    var style = new Style();
    style.setName("Baremaps");
    style.setSources(java.util.Map.of("baremaps", source));
    return style;
  }

  private static Tileset tileset() {
    var tileset = new Tileset();
    tileset.setTilejson("2.2.0");
    tileset.setName("Baremaps");
    tileset.setTiles(List.of(TILEJSON_URL));
    return tileset;
  }

  private static void write(Path path, Object config) throws Exception {
    Files.write(path, objectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(config));
  }
}
