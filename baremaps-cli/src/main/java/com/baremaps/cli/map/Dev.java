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

import com.baremaps.cli.BaremapsException;
import com.baremaps.cli.WebServer;
import com.baremaps.config.ConfigReader;
import com.baremaps.maplibre.style.Style;
import com.baremaps.maplibre.tileset.Tileset;
import com.baremaps.postgres.utils.PostgresUtils;
import com.baremaps.server.ChangeResource;
import com.baremaps.server.StyleResource;
import com.baremaps.server.TilesetResource;
import com.baremaps.server.VectorTileResource;
import com.baremaps.tilestore.postgres.PostgresTileStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "dev",
    description = "Start a development server with live reload.")
public class Dev implements Callable<Integer> {

  private final ConfigReader configReader = new ConfigReader();

  @Option(names = {"--tileset"}, paramLabel = "TILESET", description = "The tileset file.",
      required = true)
  private Path tilesetPath;

  @Option(names = {"--style"}, paramLabel = "STYLE", description = "The style file.",
      required = true)
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
    var datasource = PostgresUtils.createDataSourceFromObject(tileset().getDatabase());
    var postgresVersion = PostgresUtils.getPostgresVersion(datasource);

    new WebServer(host, port)
        .resource(new ChangeResource(tilesetPath, stylePath))
        .resource("/tiles", new VectorTileResource(
            () -> new PostgresTileStore(datasource, tileset(), postgresVersion)))
        .resource(new StyleResource(this::style))
        .resource(new TilesetResource(this::tileset))
        .files("/static", "viewer.html")
        .assets(assetsPath)
        .run();

    return 0;
  }

  // The configuration files are read again on every request, and the tile store rebuilt from them,
  // so that an edit shows up in the browser without a restart. This is what makes this command a
  // development server rather than the serve command. The connection to the database is the one
  // thing that is kept, as the tileset is not expected to be pointed at another database midway.

  private Tileset tileset() {
    return read(tilesetPath, Tileset.class);
  }

  private Style style() {
    return read(stylePath, Style.class);
  }

  private <T> T read(Path path, Class<T> type) {
    try {
      return configReader.read(path, type);
    } catch (IOException e) {
      throw new BaremapsException(e);
    }
  }
}
