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

package com.baremaps.cli.dem;

import static com.baremaps.utils.ObjectMapperUtils.objectMapper;

import com.baremaps.dem.ElevationUtils;
import com.baremaps.server.BufferedImageResource;
import com.baremaps.server.VectorTileResource;
import com.baremaps.tilestore.raster.*;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.annotation.JacksonResponseConverterFunction;
import com.linecorp.armeria.server.cors.CorsService;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.file.HttpFile;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * A command that starts a tile server to preview elevation data. The server serves raster tiles for
 * elevation and hillshade data, and vector tiles for contour and hillshade data.
 */
@Command(name = "serve", description = "Start a tile server to preview elevation data.")
public class Serve implements Callable<Integer> {

  @Option(names = {"--host"}, paramLabel = "HOST", description = "The host of the server.")
  private String host = "localhost";

  @Option(names = {"--port"}, paramLabel = "PORT", description = "The port of the server.")
  private int port = 9000;

  @Option(names = {"--path"}, paramLabel = "PATH",
      description = "The path of a digital elevation model (DEM) file in the geotiff format.")
  private Path path;

  @Override
  public Integer call() throws Exception {
    // Initialize the tile stores
    var geoTiffReader = new GeoTiffReader(path);
    var rasterElevationTileStore = new TerrariumTileStore(geoTiffReader);
    var rasterHillshadeTileStore =
        new RasterHillshadeTileStore(
            geoTiffReader,
            ElevationUtils::terrariumToElevation);
    var vectorHillshadeTileStore =
        new VectorHillshadeTileStore(
            geoTiffReader);
    var vectorContourTileStore =
        new VectorContourTileStore(geoTiffReader);

    // Initialize the server
    var objectMapper = objectMapper();
    var jsonResponseConverter = new JacksonResponseConverterFunction(objectMapper);
    var serverBuilder = Server.builder();
    serverBuilder.http(port);

    // Register the services
    serverBuilder.annotatedService(
        "/raster/elevation",
        new BufferedImageResource(() -> rasterElevationTileStore),
        jsonResponseConverter);
    serverBuilder.annotatedService(
        "/raster/hillshade",
        new BufferedImageResource(() -> rasterHillshadeTileStore),
        jsonResponseConverter);
    serverBuilder.annotatedService(
        "/vector/contour",
        new VectorTileResource(() -> vectorContourTileStore),
        jsonResponseConverter);
    serverBuilder.annotatedService(
        "/vector/hillshade",
        new VectorTileResource(() -> vectorHillshadeTileStore),
        jsonResponseConverter);

    var index = HttpFile.of(ClassLoader.getSystemClassLoader(), "/dem/index.html");
    serverBuilder.service("/", index.asService());

    serverBuilder.decorator(CorsService.builderForAnyOrigin()
        .allowAllRequestHeaders(true)
        .allowRequestMethods(
            HttpMethod.GET,
            HttpMethod.POST,
            HttpMethod.PUT,
            HttpMethod.DELETE,
            HttpMethod.OPTIONS,
            HttpMethod.HEAD)
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

    return 0;
  }
}
