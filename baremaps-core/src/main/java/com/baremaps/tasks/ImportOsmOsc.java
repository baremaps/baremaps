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

import static com.baremaps.data.stream.ConsumerUtils.consumeThenReturn;

import com.baremaps.openstreetmap.GeometryOptions;
import com.baremaps.openstreetmap.function.ChangeEntitiesHandler;
import com.baremaps.openstreetmap.model.Entity;
import com.baremaps.openstreetmap.xml.XmlChangeReader;
import com.baremaps.postgres.openstreetmap.CopyChangeImporter;
import com.baremaps.postgres.openstreetmap.NodeRepository;
import com.baremaps.postgres.openstreetmap.RelationRepository;
import com.baremaps.postgres.openstreetmap.WayRepository;
import com.baremaps.utils.Compression;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import an OSM OSC file into a database.
 */
public class ImportOsmOsc implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ImportOsmOsc.class);

  private Path file;
  private Compression compression;
  private Object database;
  private Integer databaseSrid;

  /**
   * Constructs a {@code ImportOsmOsc}.
   */
  public ImportOsmOsc() {

  }

  /**
   * Constructs an {@code ImportOsmOsc}.
   *
   * @param file the OSM OSC file
   * @param compression the compression
   * @param database the database
   * @param databaseSrid the database SRID
   */
  public ImportOsmOsc(Path file, Compression compression, Object database, Integer databaseSrid) {
    this.file = file;
    this.compression = compression;
    this.database = database;
    this.databaseSrid = databaseSrid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(WorkflowContext context) throws Exception {
    var path = file.toAbsolutePath();

    // Initialize the repositories
    var datasource = context.getDataSource(database);
    var nodeRepository = new NodeRepository(datasource);
    var wayRepository = new WayRepository(datasource);
    var relationRepository = new RelationRepository(datasource);

    try (var coordinateMap = context.getCoordinateMap();
        var referenceMap = context.getReferenceMap()) {
      execute(path, compression, coordinateMap, referenceMap, nodeRepository, wayRepository,
          relationRepository, databaseSrid);
    }
  }

  static void execute(Path path, Compression compression, Map<Long, Coordinate> coordinateMap,
      Map<Long, List<Long>> referenceMap, NodeRepository nodeRepository,
      WayRepository wayRepository, RelationRepository relationRepository, Integer databaseSrid)
      throws IOException {
    var geometryOptions = new GeometryOptions(coordinateMap, referenceMap, databaseSrid);
    var prepareChange = consumeThenReturn(
        new ChangeEntitiesHandler<>(Entity.class, geometryOptions.entityHandler()));
    var importChange = new CopyChangeImporter(nodeRepository, wayRepository, relationRepository);

    try (var changeInputStream =
        new BufferedInputStream(compression.decompress(Files.newInputStream(path)))) {
      new XmlChangeReader().read(changeInputStream).map(prepareChange).forEach(importChange);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new StringJoiner(", ", ImportOsmOsc.class.getSimpleName() + "[", "]")
        .add("file=" + file)
        .add("compression=" + compression)
        .add("database=" + database)
        .add("databaseSrid=" + databaseSrid)
        .toString();
  }
}
