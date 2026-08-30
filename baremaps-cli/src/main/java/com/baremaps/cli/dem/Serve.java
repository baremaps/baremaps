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

import com.baremaps.cli.WebServer;
import com.baremaps.server.BufferedImageResource;
import com.baremaps.server.VectorTileResource;
import com.baremaps.tilestore.raster.GeoTiffReader;
import com.baremaps.tilestore.raster.RasterHillshadeTileStore;
import com.baremaps.tilestore.raster.TerrariumTileStore;
import com.baremaps.tilestore.raster.VectorContourTileStore;
import com.baremaps.tilestore.raster.VectorHillshadeTileStore;
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

  // The server listens on every interface, as it is often reached from outside the machine or the
  // container that runs it.
  @Option(names = {"--host"}, paramLabel = "HOST", description = "The host of the server.")
  private String host = "0.0.0.0";

  @Option(names = {"--port"}, paramLabel = "PORT", description = "The port of the server.")
  private int port = 9000;

  @Option(names = {"--path"}, paramLabel = "PATH", required = true,
      description = "The path of a digital elevation model (DEM) file in the geotiff format.")
  private Path path;

  @Override
  public Integer call() throws Exception {
    try (var geoTiffReader = new GeoTiffReader(path);
        var rasterElevation = new TerrariumTileStore(geoTiffReader);
        var rasterHillshade = new RasterHillshadeTileStore(geoTiffReader);
        var vectorContour = new VectorContourTileStore(geoTiffReader);
        var vectorHillshade = new VectorHillshadeTileStore(geoTiffReader)) {
      new WebServer(host, port)
          .resource("/raster/elevation", new BufferedImageResource(() -> rasterElevation))
          .resource("/raster/hillshade", new BufferedImageResource(() -> rasterHillshade))
          .resource("/vector/contour", new VectorTileResource(() -> vectorContour))
          .resource("/vector/hillshade", new VectorTileResource(() -> vectorHillshade))
          .files("/dem", "index.html")
          .run();
    }
    return 0;
  }
}
