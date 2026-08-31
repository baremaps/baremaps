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

import com.baremaps.config.ConfigReader;
import com.baremaps.maplibre.map.MapCompiler;
import com.baremaps.maplibre.map.MapSpec;
import com.baremaps.maplibre.map.SchemaCompiler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Writes the style and the tileset derived from a map specification.
 *
 * <p>
 * The serving commands derive them in memory, so this exists to make the derivation legible: the
 * attributes a query selects are computed from the style rather than written down, and being able
 * to read the result is what makes that reviewable in a diff.
 */
@Command(
    name = "compile",
    description = "Derive the style and the tileset from a map specification.")
public class Compile implements Callable<Integer> {

  private static final Logger logger = LoggerFactory.getLogger(Compile.class);

  private final ConfigReader configReader = new ConfigReader();

  @Option(names = {"--map"}, paramLabel = "MAP", description = "The map specification file.",
      required = true)
  private Path mapPath;

  @Option(names = {"--style"}, paramLabel = "STYLE", description = "The style file to write.")
  private Path stylePath;

  @Option(names = {"--tileset"}, paramLabel = "TILESET", description = "The tileset file to write.")
  private Path tilesetPath;

  @Option(names = {"--sql"}, paramLabel = "SQL",
      description = "The generalization schema file to write.")
  private Path sqlPath;

  @Override
  public Integer call() throws Exception {
    var spec = configReader.read(mapPath, MapSpec.class);

    if (stylePath != null) {
      write(stylePath, MapCompiler.style(spec));
      logger.info("Style written: {}", stylePath);
    }
    if (tilesetPath != null) {
      write(tilesetPath, MapCompiler.tileset(spec));
      logger.info("Tileset written: {}", tilesetPath);
    }
    if (sqlPath != null) {
      Files.writeString(sqlPath, SchemaCompiler.sql(spec));
      logger.info("Schema written: {}", sqlPath);
    }
    if (stylePath == null && tilesetPath == null && sqlPath == null) {
      logger.info("Nothing to write: pass --style, --tileset, --sql, or several.");
    }
    return 0;
  }

  private static void write(Path path, Object config) throws Exception {
    Files.write(path, objectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(config));
  }
}
