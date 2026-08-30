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

import com.baremaps.cli.WebServer;
import com.baremaps.config.ConfigReader;
import com.baremaps.maplibre.style.Style;
import com.baremaps.maplibre.tilejson.TileJSON;
import com.baremaps.server.StyleResource;
import com.baremaps.server.TileJSONResource;
import com.baremaps.server.VectorTileResource;
import com.baremaps.tilestore.mbtiles.MBTilesStore;
import com.baremaps.tilestore.vector.VectorTileCache;
import com.baremaps.utils.SqliteUtils;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "mbtiles",
    description = "Start a mbtiles server with caching capabilities.")
public class MBTiles implements Callable<Integer> {

  @Option(names = {"--cache"}, paramLabel = "CACHE", description = "The caffeine cache directive.")
  private String cache = "";

  @Option(names = {"--mbtiles"}, paramLabel = "MBTILES", description = "The mbtiles file.",
      required = true)
  private Path mbtilesPath;

  @Option(names = {"--tilejson"}, paramLabel = "TILEJSON", description = "The tileJSON file.",
      required = true)
  private Path tileJSONPath;

  @Option(names = {"--style"}, paramLabel = "STYLE", description = "The style file.",
      required = true)
  private Path stylePath;

  // The server listens on every interface, as it is often reached from outside the machine or the
  // container that runs it.
  @Option(names = {"--host"}, paramLabel = "HOST", description = "The host of the server.")
  private String host = "0.0.0.0";

  @Option(names = {"--port"}, paramLabel = "PORT", description = "The port of the server.")
  private int port = 9000;

  @Override
  public Integer call() throws Exception {
    var configReader = new ConfigReader();
    var style = configReader.read(stylePath, Style.class);
    var tileJSON = configReader.read(tileJSONPath, TileJSON.class);

    var datasource = SqliteUtils.createDataSource(mbtilesPath, true);
    try (var tileStore = new MBTilesStore(datasource);
        var tileCache = new VectorTileCache(tileStore, CaffeineSpec.parse(cache))) {
      new WebServer(host, port)
          .resource("/tiles", new VectorTileResource(() -> tileCache))
          .resource(new StyleResource(() -> style))
          .resource(new TileJSONResource(() -> tileJSON))
          .files("/static", "server.html")
          .run();
    }
    return 0;
  }
}
