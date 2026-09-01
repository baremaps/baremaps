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

import com.baremaps.cli.BaremapsException;
import com.baremaps.cli.WebServer;
import com.baremaps.server.BufferedImageResource;
import com.baremaps.server.VectorTileResource;
import com.baremaps.tilestore.pmtiles.PMTilesStore;
import com.baremaps.tilestore.raster.ElevationReader;
import com.baremaps.tilestore.raster.GeoTiffReader;
import com.baremaps.tilestore.raster.RasterHillshadeTileStore;
import com.baremaps.tilestore.raster.TerrariumElevationReader;
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

  @Option(names = {"--path"}, paramLabel = "PATH",
      description = "The path of a digital elevation model (DEM) file in the geotiff format.")
  private Path path;

  @Option(names = {"--pmtiles"}, paramLabel = "PMTILES",
      description = "The path of a PMTiles archive of terrarium tiles, such as Mapterhorn's.")
  private Path pmtilesPath;

  @Option(names = {"--pmtiles-tile-size"}, paramLabel = "SIZE",
      description = "The side of a tile of that archive, in pixels.")
  private int pmtilesTileSize = TerrariumElevationReader.DEFAULT_TILE_SIZE;

  @Option(names = {"--pmtiles-maxzoom"}, paramLabel = "MAXZOOM",
      description = "The deepest zoom level that archive holds.")
  private int pmtilesMaxzoom = 12;

  @Override
  public Integer call() throws Exception {
    try (var elevation = elevation();
        var rasterElevation = new TerrariumTileStore(elevation);
        var rasterHillshade = new RasterHillshadeTileStore(elevation);
        var vectorContour = new VectorContourTileStore(elevation);
        var vectorHillshade = new VectorHillshadeTileStore(elevation)) {
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

  /**
   * The elevation to preview: a GeoTIFF covering a region, or an archive of terrarium tiles
   * covering the planet. Both resolve to the same grid, so the four stores above are the same
   * either way.
   */
  private ElevationReader elevation() throws Exception {
    if ((path == null) == (pmtilesPath == null)) {
      throw new BaremapsException("Pass --path or --pmtiles, but not both.");
    }
    if (path != null) {
      return new GeoTiffReader(path);
    }
    return new TerrariumElevationReader(
        new PMTilesStore(pmtilesPath), pmtilesTileSize, pmtilesMaxzoom);
  }
}
