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

import com.baremaps.cli.Options;
import com.baremaps.config.ConfigReader;
import com.baremaps.maplibre.style.Style;
import com.baremaps.maplibre.tilejson.TileJSON;
import com.baremaps.maplibre.tileset.Tileset;
import com.baremaps.postgres.utils.PostgresUtils;
import com.baremaps.server.SearchResource;
import com.baremaps.server.StyleResource;
import com.baremaps.server.TileJSONResource;
import com.baremaps.server.VectorTileResource;
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.postgres.PostgresTileStore;
import com.baremaps.tilestore.vector.VectorTileCache;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.annotation.JacksonResponseConverterFunction;
import com.linecorp.armeria.server.cors.CorsService;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.file.FileService;
import com.linecorp.armeria.server.file.HttpFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(
    name = "serve",
    description = "Start a tile server with caching capabilities.")
@SuppressWarnings("squid:S106")
public class Serve implements Callable<Integer> {

  private static final Logger logger = LoggerFactory.getLogger(Serve.class);

  @Mixin
  private Options options;

  @Option(names = {"--cache"}, paramLabel = "CACHE", description = {
      "The caffeine specification of the cache. " +
          "For instance, 'maximumWeight=1073741824,expireAfterAccess=1h' " +
          "sets a 1GB cache whose entries expires after one hour."})
  private String cache = "";

  @Option(names = {"--tileset"}, paramLabel = "TILESET", description = "The tileset file.",
      required = true)
  private Path tilesetPath;

  @Option(names = {"--style"}, paramLabel = "STYLE", description = "The style file.",
      required = true)
  private Path stylePath;

  @Option(names = {"--assets"}, paramLabel = "ASSETS", description = "The assets directory.",
      required = false)
  private Path assetsPath;

  @Option(names = {"--host"}, paramLabel = "HOST", description = "The host of the server.")
  private String host = "localhost";

  @Option(names = {"--port"}, paramLabel = "PORT", description = "The port of the server.")
  private int port = 9000;

  @Override
  public Integer call() throws Exception {
    var objectMapper = objectMapper();
    var configReader = new ConfigReader();
    var caffeineSpec = CaffeineSpec.parse(cache);
    var tileset = objectMapper.readValue(configReader.read(tilesetPath), Tileset.class);
    var datasource = PostgresUtils.createDataSourceFromObject(tileset.getDatabase());
    var postgresVersion = PostgresUtils.getPostgresVersion(datasource);

    try (
        var tileStore = new PostgresTileStore(datasource, tileset, postgresVersion);
        var tileCache = new VectorTileCache(tileStore, caffeineSpec)) {

      var tileStoreSupplier = (Supplier<TileStore<ByteBuffer>>) () -> tileCache;

      var style = objectMapper.readValue(configReader.read(stylePath), Style.class);
      var styleSupplier = (Supplier<Style>) () -> style;

      var tileJSON = objectMapper.readValue(configReader.read(tilesetPath), TileJSON.class);
      var tileJSONSupplier = (Supplier<TileJSON>) () -> tileJSON;

      var serverBuilder = Server.builder();
      serverBuilder.http(port);

      var jsonResponseConverter = new JacksonResponseConverterFunction(objectMapper);
      serverBuilder.annotatedService("/tiles", new VectorTileResource(tileStoreSupplier),
          jsonResponseConverter);
      serverBuilder.annotatedService(new StyleResource(styleSupplier), jsonResponseConverter);
      serverBuilder.annotatedService(new TileJSONResource(tileJSONSupplier), jsonResponseConverter);
      serverBuilder.annotatedService(new SearchResource(datasource), jsonResponseConverter);

      var index = HttpFile.of(ClassLoader.getSystemClassLoader(), "/static/server.html");
      serverBuilder.service("/", index.asService());
      serverBuilder.serviceUnder("/",
          FileService.of(ClassLoader.getSystemClassLoader(), "/static"));

      if (assetsPath != null) {
        serverBuilder.serviceUnder("/assets", FileService.of(assetsPath));
      }

      serverBuilder.decorator(CorsService.builderForAnyOrigin()
          .allowRequestMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE,
              HttpMethod.OPTIONS, HttpMethod.HEAD)
          .allowRequestHeaders(HttpHeaderNames.ORIGIN, HttpHeaderNames.CONTENT_TYPE,
              HttpHeaderNames.ACCEPT, HttpHeaderNames.AUTHORIZATION)
          .allowCredentials()
          .exposeHeaders(HttpHeaderNames.LOCATION)
          .newDecorator());

      serverBuilder.serviceUnder("/docs", new DocService());

      serverBuilder.disableServerHeader();
      serverBuilder.disableDateHeader();

      var server = serverBuilder.build();

      var startFuture = server.start();
      startFuture.join();

      var shutdownFuture = server.closeOnJvmShutdown();
      shutdownFuture.join();
    }

    return 0;
  }
}
