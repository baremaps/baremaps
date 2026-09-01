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
import com.baremaps.postgres.utils.PostgresUtils;
import com.baremaps.server.SearchResource;
import com.baremaps.server.StyleResource;
import com.baremaps.server.TileJSONResource;
import com.baremaps.server.VectorTileResource;
import com.baremaps.tilestore.postgres.PostgresTileStore;
import com.baremaps.tilestore.vector.VectorTileCache;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "serve",
    description = "Start a tile server with caching capabilities.")
public class Serve implements Callable<Integer> {

  @Option(names = {"--cache"}, paramLabel = "CACHE", description = {
      "The caffeine specification of the cache. " +
          "For instance, 'maximumWeight=1073741824,expireAfterAccess=1h' " +
          "sets a 1GB cache whose entries expires after one hour."})
  private String cache = "";

  @Option(names = {"--map"}, paramLabel = "MAP",
      description = "The map specification file, from which the style and the tileset are derived.")
  private Path mapPath;

  @Option(names = {"--tileset"}, paramLabel = "TILESET",
      description = "The tileset file. Superseded by --map.")
  private Path tilesetPath;

  @Option(names = {"--style"}, paramLabel = "STYLE",
      description = "The style file. Superseded by --map.")
  private Path stylePath;

  @Option(names = {"--assets"}, paramLabel = "ASSETS", description = "The assets directory.")
  private Path assetsPath;

  // The server listens on every interface, as it is often reached from outside the machine or the
  // container that runs it.
  @Option(names = {"--host"}, paramLabel = "HOST", description = "The host of the server.")
  private String host = "0.0.0.0";

  @Option(names = {"--port"}, paramLabel = "PORT", description = "The port of the server.")
  private int port = 9000;

  @Override
  public Integer call() throws Exception {
    MapInput.validate(mapPath, tilesetPath, stylePath);

    var configReader = new ConfigReader();
    var tileset = MapInput.tileset(configReader, mapPath, tilesetPath);
    var style = MapInput.style(configReader, mapPath, stylePath);
    var tileJSON = MapInput.tileJSON(configReader, mapPath, tilesetPath);

    var datasource = PostgresUtils.createDataSourceFromObject(tileset.getDatabase());
    var postgresVersion = PostgresUtils.getPostgresVersion(datasource);

    var terrain = MapInput.terrain(configReader, mapPath);

    try (var tileStore = new PostgresTileStore(datasource, tileset, postgresVersion);
        var tileCache = new VectorTileCache(tileStore, CaffeineSpec.parse(cache));
        var terrainTileStore = terrain == null ? null : MapInput.terrainTileStore(terrain);
        // Tracing a terrain tile costs far more than reading one from the database, so it is worth
        // caching even though its input never changes.
        var terrainCache = terrainTileStore == null ? null
            : new VectorTileCache(terrainTileStore, CaffeineSpec.parse(cache))) {
      var server = new WebServer(host, port)
          .resource("/tiles", new VectorTileResource(() -> tileCache))
          .resource(new StyleResource(() -> style))
          .resource(new TileJSONResource(() -> tileJSON))
          .resource(new SearchResource(datasource));
      if (terrainCache != null) {
        server.resource("/terrain", new VectorTileResource(() -> terrainCache));
      }
      server.files("/static", "server.html").assets(assetsPath).run();
    }
    return 0;
  }
}
