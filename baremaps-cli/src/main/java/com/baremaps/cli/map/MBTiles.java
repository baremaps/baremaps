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
import com.baremaps.server.*;
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.mbtiles.MBTilesStore;
import com.baremaps.tilestore.vector.VectorTileCache;
import com.baremaps.utils.SqliteUtils;
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
    name = "mbtiles",
    description = "Start a mbtiles server with caching capabilities.")
@SuppressWarnings("squid:S106")
public class MBTiles implements Callable<Integer> {

  private static final Logger logger = LoggerFactory.getLogger(MBTiles.class);

  @Mixin
  private Options options;

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

  @Option(names = {"--port"}, paramLabel = "PORT", description = "The port of the server.")
  private int port = 9000;

  @Override
  public Integer call() throws Exception {
    var objectMapper = objectMapper();
    var configReader = new ConfigReader();
    var caffeineSpec = CaffeineSpec.parse(cache);

    var datasource = SqliteUtils.createDataSource(mbtilesPath, true);
    try (var tileStore = new MBTilesStore(datasource);
        var tileCache = new VectorTileCache(tileStore, caffeineSpec)) {
      var tileStoreSupplier = (Supplier<TileStore<ByteBuffer>>) () -> tileCache;

      var style = objectMapper.readValue(configReader.read(stylePath), Style.class);
      var styleSupplier = (Supplier<Style>) () -> style;

      var tileJSON = objectMapper.readValue(configReader.read(tileJSONPath), TileJSON.class);
      var tileJSONSupplier = (Supplier<TileJSON>) () -> tileJSON;

      var serverBuilder = Server.builder();
      serverBuilder.http(port);

      var jsonResponseConverter = new JacksonResponseConverterFunction(objectMapper);
      serverBuilder.annotatedService("/tiles", new VectorTileResource(tileStoreSupplier),
          jsonResponseConverter);
      serverBuilder.annotatedService(new StyleResource(styleSupplier), jsonResponseConverter);
      serverBuilder.annotatedService(new TileJSONResource(tileJSONSupplier), jsonResponseConverter);

      var index = HttpFile.of(ClassLoader.getSystemClassLoader(), "/static/server.html");
      serverBuilder.service("/", index.asService());
      serverBuilder.serviceUnder("/",
          FileService.of(ClassLoader.getSystemClassLoader(), "/static"));

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
