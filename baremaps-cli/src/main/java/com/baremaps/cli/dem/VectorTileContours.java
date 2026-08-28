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
import com.baremaps.openstreetmap.stream.ProgressLogger;
import com.baremaps.openstreetmap.stream.StreamUtils;
import com.baremaps.tasks.ExportVectorTiles;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileEntry;
import com.baremaps.tilestore.TileStoreException;
import com.baremaps.tilestore.pmtiles.PMTilesStore;
import com.baremaps.tilestore.raster.*;
import com.baremaps.workflow.WorkflowException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import org.locationtech.jts.geom.Envelope;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "vector-contours", description = "Generate vector contours from a DEM.")
@SuppressWarnings({"squid:S106", "squid:S3864"})
public class VectorTileContours implements Callable<Integer> {

  @Option(names = {"--path"}, paramLabel = "PATH", description = "The path of a geoTIFF file.")
  private Path path;

  @Option(names = {"--repository"}, paramLabel = "REPOSITORY", description = "The tile repository.",
      required = true)
  private Path repository;

  @Option(names = {"--format"}, paramLabel = "FORMAT",
      description = "The format of the repository.")
  private ExportVectorTiles.Format format = ExportVectorTiles.Format.FILE;


  @Override
  public Integer call() throws Exception {
    var contourLayer = new TilesetLayer();
    contourLayer.setId("contours");

    var tileset = new Tileset();
    tileset.setName("contours");
    tileset.setMinzoom(2);
    tileset.setMaxzoom(10);
    tileset.setCenter(List.of(0d, 0d, 1d));
    tileset.setBounds(List.of(-180d, -85.0511d, 180d, 85.0511d));
    tileset.setVectorLayers(List.of(contourLayer));

    // Initialize the tile stores
    try (var geoTiffReader = new GeoTiffReader(path);
        var sourceTileStore = new VectorContourTileStore(geoTiffReader);
        var targetTileStore = new PMTilesStore(repository, tileset);) {

      var envelope = new Envelope(-180, 180, -85.0511, 85.0511);
      var count = TileCoord.count(envelope, 0, 10);

      var tileCoordIterator =
          TileCoord.iterator(envelope, 2, 10);
      var tileCoordStream =
          StreamUtils.stream(tileCoordIterator).peek(new ProgressLogger<>(count, 1000));


      var bufferedTileEntryStream = StreamUtils.bufferInCompletionOrder(tileCoordStream, tile -> {

        try {
          return new TileEntry<>(tile, sourceTileStore.read(tile));
        } catch (TileStoreException e) {
          throw new WorkflowException(e);
        } finally {
          System.out.println("Processing tile " + tile);
        }
      }, 8);

      var partitionedTileEntryStream = StreamUtils.partition(bufferedTileEntryStream, 8);
      partitionedTileEntryStream.forEach(batch -> {
        try {
          targetTileStore.write(batch);
        } catch (TileStoreException e) {
          throw new WorkflowException(e);
        }
      });
      return 0;
    }
  }
}
