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

package com.baremaps.tasks;

import static com.baremaps.utils.ObjectMapperUtils.objectMapper;

import com.baremaps.config.ConfigReader;
import com.baremaps.maplibre.style.Style;
import com.baremaps.maplibre.tileset.Tileset;
import com.baremaps.maplibre.tileset.TilesetQuery;
import com.baremaps.openstreetmap.stream.ProgressLogger;
import com.baremaps.openstreetmap.stream.StreamUtils;
import com.baremaps.postgres.utils.PostgresUtils;
import com.baremaps.tilestore.*;
import com.baremaps.tilestore.file.FileTileStore;
import com.baremaps.tilestore.mbtiles.MBTilesStore;
import com.baremaps.tilestore.pmtiles.PMTilesStore;
import com.baremaps.tilestore.postgres.PostgresTileStore;
import com.baremaps.utils.SqliteUtils;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import com.baremaps.workflow.WorkflowException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.locationtech.jts.geom.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Export vector tiles from a tileset.
 */
public class ExportVectorTiles implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ExportVectorTiles.class);

  public enum Format {
    FILE,
    MBTILES,
    PMTILES
  }

  private Path tileset;

  private Path style;

  private Path repository;

  private Format format;

  /**
   * Constructs a {@code ExportVectorTiles}.
   */
  public ExportVectorTiles() {

  }

  /**
   * Constructs a {@code ExportVectorTiles}.
   *
   * @param tileset the tileset
   * @param repository the repository
   * @param format the format
   */
  public ExportVectorTiles(Path tileset, Path style, Path repository, Format format) {
    this.tileset = tileset;
    this.style = style;
    this.repository = repository;
    this.format = format;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(WorkflowContext context) throws Exception {
    var configReader = new ConfigReader();
    var objectMapper = objectMapper();

    var tilesetObject = objectMapper.readValue(configReader.read(this.tileset), Tileset.class);
    var styleObject = objectMapper.readValue(configReader.read(this.style), Style.class);

    // Write the static files
    var directory = switch (format) {
      case FILE -> repository;
      case MBTILES -> repository.getParent();
      case PMTILES -> repository.getParent();
    };
    Files.createDirectories(directory);
    try (var html = this.getClass().getResourceAsStream("/static/server.html")) {
      Files.write(
          directory.resolve("index.html"),
          html.readAllBytes(),
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    }
    Files.write(
        directory.resolve("tiles.json"),
        objectMapper.writeValueAsBytes(tilesetObject),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
    Files.write(
        directory.resolve("style.json"),
        objectMapper.writeValueAsBytes(styleObject),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);

    var datasource = context.getDataSource(tilesetObject.getDatabase());

    try (var sourceTileStore = sourceTileStore(tilesetObject, datasource);
        var targetTileStore = targetTileStore(tilesetObject)) {

      var envelope = tilesetObject.getBounds().size() == 4
          ? new Envelope(
              tilesetObject.getBounds().get(0), tilesetObject.getBounds().get(2),
              tilesetObject.getBounds().get(1), tilesetObject.getBounds().get(3))
          : new Envelope(-180, 180, -85.0511, 85.0511);

      var count = TileCoord.count(envelope, tilesetObject.getMinzoom(), tilesetObject.getMaxzoom());
      var start = System.currentTimeMillis();

      var tileCoordIterator =
          TileCoord.iterator(envelope, tilesetObject.getMinzoom(), tilesetObject.getMaxzoom());
      var tileCoordStream =
          StreamUtils.stream(tileCoordIterator).peek(new ProgressLogger<>(count, 5000));

      var bufferedTileEntryStream = StreamUtils.bufferInCompletionOrder(tileCoordStream, tile -> {
        try {
          return new TileEntry<>(tile, sourceTileStore.read(tile));
        } catch (TileStoreException e) {
          throw new WorkflowException(e);
        }
      }, 1000);

      var partitionedTileEntryStream = StreamUtils.partition(bufferedTileEntryStream, 1000);
      partitionedTileEntryStream.forEach(batch -> {
        try {
          targetTileStore.write(batch);
        } catch (TileStoreException e) {
          throw new WorkflowException(e);
        }
      });

      var stop = System.currentTimeMillis();
      logger.info("Exported {} tiles in {}s", count, (stop - start) / 1000);
    }
  }

  private TileStore<ByteBuffer> sourceTileStore(Tileset tileset, DataSource datasource)
      throws SQLException {
    var postgresVersion = PostgresUtils.getPostgresVersion(datasource);
    return new PostgresTileStore(datasource, tileset, postgresVersion);
  }

  private TileStore<ByteBuffer> targetTileStore(Tileset source)
      throws TileStoreException, IOException {
    switch (format) {
      case FILE:
        return new FileTileStore(repository.resolve("tiles"));
      case MBTILES:
        Files.deleteIfExists(repository);
        var dataSource = SqliteUtils.createDataSource(repository, false);
        var tilesStore = new MBTilesStore(dataSource);
        tilesStore.initializeDatabase();
        tilesStore.writeMetadata(metadata(source));
        return tilesStore;
      case PMTILES:
        Files.deleteIfExists(repository);
        return new PMTilesStore(repository, source);
      default:
        throw new IllegalArgumentException("Unsupported format");
    }
  }

  private Map<String, String> metadata(Tileset tileset) throws JsonProcessingException {
    var metadata = new HashMap<String, String>();

    metadata.put("name", tileset.getName());
    metadata.put("version", tileset.getVersion());
    metadata.put("description", tileset.getDescription());
    metadata.put("attribution", tileset.getAttribution());
    metadata.put("type", "baselayer");
    metadata.put("format", "pbf");
    metadata.put(
        "center",
        tileset.getCenter().stream().map(Number::toString).collect(Collectors.joining(", ")));
    metadata.put(
        "bounds",
        tileset.getBounds().stream().map(Object::toString).collect(Collectors.joining(", ")));
    metadata.put("minzoom", Double.toString(tileset.getMinzoom()));
    metadata.put("maxzoom", Double.toString(tileset.getMaxzoom()));

    var layers =
        tileset.getVectorLayers().stream()
            .map(
                layer -> {
                  Map<String, Object> map = new HashMap<>();
                  map.put("id", layer.getId());
                  map.put("description", layer.getDescription());
                  map.put(
                      "minzoom",
                      layer.getQueries().stream().mapToInt(TilesetQuery::getMinzoom).min()
                          .getAsInt());
                  map.put(
                      "maxzoom",
                      layer.getQueries().stream().mapToInt(TilesetQuery::getMaxzoom).max()
                          .getAsInt());
                  return map;
                })
            .toList();

    metadata.put("json", new ObjectMapper().writeValueAsString(layers));

    return metadata;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new StringJoiner(", ", ExportVectorTiles.class.getSimpleName() + "[", "]")
        .add("tileset=" + tileset)
        .add("repository=" + repository)
        .add("format=" + format)
        .toString();
  }
}
