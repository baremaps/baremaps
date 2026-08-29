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

import static java.lang.Boolean.TRUE;

import com.baremaps.data.stream.StreamUtils;
import com.baremaps.openstreetmap.GeometryOptions;
import com.baremaps.openstreetmap.model.Node;
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.openstreetmap.model.Way;
import com.baremaps.openstreetmap.pbf.PbfBlockReader;
import com.baremaps.postgres.openstreetmap.*;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.locationtech.jts.geom.Coordinate;

/**
 * Import an OSM PBF file into a database.
 */
public class ImportOsmPbf implements Task {

  private Path file;
  private Object database;
  private Integer databaseSrid;
  private Boolean replaceExisting;
  private Boolean truncateTables;

  /**
   * Constructs a {@code ImportOsmPbf}.
   */
  public ImportOsmPbf() {
    // Default constructor
  }

  /**
   * Constructs an {@code ImportOsmPbf}.
   *
   * @param file the OSM PBF file
   * @param database the database
   * @param databaseSrid the database SRID
   * @param replaceExisting whether to replace the existing tables
   */
  public ImportOsmPbf(Path file, Object database,
      Integer databaseSrid, Boolean replaceExisting, Boolean truncateTables) {
    this.file = file;
    this.database = database;
    this.databaseSrid = databaseSrid;
    this.replaceExisting = replaceExisting;
    this.truncateTables = truncateTables;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(WorkflowContext context) throws Exception {
    var path = file.toAbsolutePath();

    // Initialize the repositories
    var datasource = context.getDataSource(database);
    var headerRepository = new HeaderRepository(datasource);
    var nodeRepository = new NodeRepository(datasource);
    var wayRepository = new WayRepository(datasource);
    var relationRepository = new RelationRepository(datasource);

    // Drop the existing tables
    if (TRUE.equals(replaceExisting)) {
      headerRepository.drop();
      nodeRepository.drop();
      wayRepository.drop();
      relationRepository.drop();

      // Create the new tables
      headerRepository.create();
      nodeRepository.create();
      wayRepository.create();
      relationRepository.create();
    }

    // Truncate the existing tables
    if (TRUE.equals(truncateTables)) {
      headerRepository.truncate();
      nodeRepository.truncate();
      wayRepository.truncate();
      relationRepository.truncate();
    }

    // Closing the maps persists them in the cache directory for the updates that follow.
    try (var coordinateMap = context.getCoordinateMap();
        var referenceMap = context.getReferenceMap()) {
      execute(
          path,
          coordinateMap,
          referenceMap,
          headerRepository,
          nodeRepository,
          wayRepository,
          relationRepository,
          databaseSrid);
    }
  }

  /**
   * Imports an OSM PBF file into a database.
   *
   * @param path the OSM PBF file
   * @param coordinateMap the coordinate map
   * @param referenceMap the reference map
   * @param headerRepository the header repository
   * @param nodeRepository the node repository
   * @param wayRepository the way repository
   * @param relationRepository the relation repository
   * @param databaseSrid the database SRID
   * @throws IOException
   */
  @SuppressWarnings("squid:S107")
  static void execute(
      Path path,
      Map<Long, Coordinate> coordinateMap,
      Map<Long, List<Long>> referenceMap,
      HeaderRepository headerRepository,
      Repository<Long, Node> nodeRepository,
      Repository<Long, Way> wayRepository,
      Repository<Long, Relation> relationRepository,
      Integer databaseSrid) throws IOException {

    // configure the block reader
    var reader =
        new PbfBlockReader(new GeometryOptions(coordinateMap, referenceMap, databaseSrid));

    // configure the block importer
    var importer = new BlockImporter(
        headerRepository,
        nodeRepository,
        wayRepository,
        relationRepository);

    // Stream and process the blocks
    try (var input = Files.newInputStream(path)) {
      StreamUtils.parallel(reader.read(input)).forEach(importer);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new StringJoiner(", ", ImportOsmPbf.class.getSimpleName() + "[", "]")
        .add("file=" + file)
        .add("database=" + database)
        .add("databaseSrid=" + databaseSrid)
        .add("replaceExisting=" + replaceExisting)
        .toString();
  }
}
