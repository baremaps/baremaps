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
import com.baremaps.postgres.utils.PostgresUtils;
import com.baremaps.tilestore.*;
import com.baremaps.tilestore.file.FileTileStore;
import com.baremaps.tilestore.mbtiles.MBTilesStore;
import com.baremaps.tilestore.pmtiles.PMTilesStore;
import com.baremaps.tilestore.postgres.PostgresTileStore;
import com.baremaps.utils.SqliteUtils;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
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

  // The number of tiles read at once, and the number of tiles written per round trip.
  private static final int BATCH_SIZE = 1000;

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

    writeViewer(objectMapper, tilesetObject, styleObject);

    var datasource = context.getDataSource(tilesetObject.getDatabase());
    try (var sourceTileStore = sourceTileStore(tilesetObject, datasource);
        var targetTileStore = targetTileStore(tilesetObject)) {
      TileStoreUtils.copy(sourceTileStore, targetTileStore, envelope(tilesetObject),
          tilesetObject.getMinzoom(), tilesetObject.getMaxzoom(), BATCH_SIZE);
    }
  }

  /**
   * Writes the files that let a browser display the exported tiles: the viewer itself, the tileset
   * it points at, and the style it renders with.
   */
  private void writeViewer(ObjectMapper objectMapper, Tileset tileset, Style style)
      throws IOException {
    // A directory repository holds the tiles themselves; an archive is a file inside a directory.
    var directory = format == Format.FILE ? repository : repository.getParent();
    Files.createDirectories(directory);
    try (var html = this.getClass().getResourceAsStream("/static/server.html")) {
      write(directory.resolve("index.html"), html.readAllBytes());
    }
    write(directory.resolve("tiles.json"), objectMapper.writeValueAsBytes(tileset));
    write(directory.resolve("style.json"), objectMapper.writeValueAsBytes(style));
  }

  private static void write(Path file, byte[] content) throws IOException {
    Files.write(file, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }

  /** Returns the bounds of the tileset, defaulting to the whole web mercator extent. */
  private static Envelope envelope(Tileset tileset) {
    var bounds = tileset.getBounds();
    if (bounds == null || bounds.size() != 4) {
      return new Envelope(-180, 180, -85.0511, 85.0511);
    }
    return new Envelope(bounds.get(0), bounds.get(2), bounds.get(1), bounds.get(3));
  }

  private TileStore<ByteBuffer> sourceTileStore(Tileset tileset, DataSource datasource)
      throws SQLException {
    var postgresVersion = PostgresUtils.getPostgresVersion(datasource);
    return new PostgresTileStore(datasource, tileset, postgresVersion);
  }

  private TileStore<ByteBuffer> targetTileStore(Tileset source)
      throws TileStoreException, IOException {
    return switch (format) {
      case FILE -> new FileTileStore(repository.resolve("tiles"));
      case MBTILES -> {
        Files.deleteIfExists(repository);
        var tilesStore = new MBTilesStore(SqliteUtils.createDataSource(repository, false));
        tilesStore.initializeDatabase();
        tilesStore.writeMetadata(metadata(source));
        yield tilesStore;
      }
      case PMTILES -> {
        Files.deleteIfExists(repository);
        yield new PMTilesStore(repository, source);
      }
    };
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
