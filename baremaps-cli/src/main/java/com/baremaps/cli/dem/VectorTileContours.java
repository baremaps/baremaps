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

import com.baremaps.maplibre.tileset.Tileset;
import com.baremaps.maplibre.tileset.TilesetLayer;
import com.baremaps.tilestore.TileStoreUtils;
import com.baremaps.tilestore.pmtiles.PMTilesStore;
import com.baremaps.tilestore.raster.GeoTiffReader;
import com.baremaps.tilestore.raster.VectorContourTileStore;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import org.locationtech.jts.geom.Envelope;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * A command that turns a digital elevation model into a PMTiles archive of contour lines.
 */
@Command(name = "vector-contours", description = "Generate vector contours from a DEM.")
public class VectorTileContours implements Callable<Integer> {

  private static final Envelope WORLD = new Envelope(-180, 180, -85.0511, 85.0511);

  // Contours are computed from the raster, which is expensive, and only a few tiles are therefore
  // kept in flight at a time.
  private static final int BATCH_SIZE = 8;

  @Option(names = {"--path"}, paramLabel = "PATH", description = "The path of a geoTIFF file.",
      required = true)
  private Path path;

  @Option(names = {"--repository"}, paramLabel = "REPOSITORY", description = "The tile repository.",
      required = true)
  private Path repository;

  @Option(names = {"--min-zoom"}, paramLabel = "MIN_ZOOM", description = "The minimum zoom level.")
  private int minZoom = 2;

  @Option(names = {"--max-zoom"}, paramLabel = "MAX_ZOOM", description = "The maximum zoom level.")
  private int maxZoom = 10;

  @Override
  public Integer call() throws Exception {
    var tileset = tileset();
    try (var geoTiffReader = new GeoTiffReader(path);
        var source = new VectorContourTileStore(geoTiffReader);
        var target = new PMTilesStore(repository, tileset)) {
      TileStoreUtils.copy(source, target, WORLD, minZoom, maxZoom, BATCH_SIZE);
    }
    return 0;
  }

  /** Describes the archive being written, as a PMTiles archive embeds its own tileset. */
  private Tileset tileset() {
    var layer = new TilesetLayer();
    layer.setId("contours");

    var tileset = new Tileset();
    tileset.setName("contours");
    tileset.setMinzoom(minZoom);
    tileset.setMaxzoom(maxZoom);
    tileset.setCenter(List.of(0d, 0d, 1d));
    tileset.setBounds(List.of(WORLD.getMinX(), WORLD.getMinY(), WORLD.getMaxX(), WORLD.getMaxY()));
    tileset.setVectorLayers(List.of(layer));
    return tileset;
  }
}
