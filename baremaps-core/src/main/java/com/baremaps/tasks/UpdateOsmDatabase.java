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

import com.baremaps.openstreetmap.function.*;
import com.baremaps.openstreetmap.model.Header;
import com.baremaps.openstreetmap.model.Node;
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.openstreetmap.model.Way;
import com.baremaps.openstreetmap.state.StateReader;
import com.baremaps.openstreetmap.xml.XmlChangeReader;
import com.baremaps.postgres.openstreetmap.*;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.zip.GZIPInputStream;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update an OSM database based on the header data stored in the database.
 */
public class UpdateOsmDatabase implements Task {

  private static final Logger logger = LoggerFactory.getLogger(UpdateOsmDatabase.class);

  private Object database;
  private Integer databaseSrid;
  private String replicationUrl;

  /**
   * Constructs a {@code UpdateOsmDatabase}.
   */
  public UpdateOsmDatabase() {

  }

  /**
   * Constructs an {@code UpdateOsmDatabase}.
   *
   * @param database the database
   * @param databaseSrid the database SRID
   */
  public UpdateOsmDatabase(Object database, Integer databaseSrid, String replicationUrl) {
    this.database = database;
    this.databaseSrid = databaseSrid;
    this.replicationUrl = replicationUrl;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(WorkflowContext context) throws Exception {
    var datasource = context.getDataSource(database);
    var coordinateMap = new CoordinateMap(datasource);
    var referenceMap = new ReferenceMap(datasource);
    var headerRepository = new HeaderRepository(datasource);
    var nodeRepository = new NodeRepository(datasource);
    var wayRepository = new WayRepository(datasource);
    var relationRepository = new RelationRepository(datasource);
    execute(
        coordinateMap,
        referenceMap,
        headerRepository,
        nodeRepository,
        wayRepository,
        relationRepository,
        databaseSrid,
        replicationUrl);
  }

  /**
   * Executes the task.
   *
   * @param coordinateMap the coordinate map
   * @param referenceMap the reference map
   * @param headerRepository the header repository
   * @param nodeRepository the node repository
   * @param wayRepository the way repository
   * @param relationRepository the relation repository
   * @param databaseSrid the SRID
   * @throws Exception if something went wrong
   */
  @SuppressWarnings("squid:S107")
  static void execute(
      Map<Long, Coordinate> coordinateMap,
      Map<Long, List<Long>> referenceMap,
      HeaderRepository headerRepository,
      Repository<Long, Node> nodeRepository,
      Repository<Long, Way> wayRepository,
      Repository<Long, Relation> relationRepository,
      Integer databaseSrid,
      String replicationUrl) throws IOException, RepositoryException {

    // Get the latest header from the database
    var header = headerRepository.selectLatest();

    // If the replicationUrl is not provided, use the one from the latest header.
    if (replicationUrl == null) {
      replicationUrl = header.replicationUrl();
    }

    // Get the sequence number of the latest header
    var stateReader = new StateReader(replicationUrl);
    var sequenceNumber = header.replicationSequenceNumber();

    // If the replicationTimestamp is not provided, guess it from the replication timestamp.
    if (sequenceNumber <= 0) {
      var replicationTimestamp = header.replicationTimestamp();
      var state = stateReader.getStateFromTimestamp(replicationTimestamp);
      if (state.isPresent()) {
        sequenceNumber = state.get().sequenceNumber();
      }
    }

    // Increment the sequence number and get the changeset url
    var nextSequenceNumber = sequenceNumber + 1;
    var changeUrl = stateReader.getUrl(nextSequenceNumber, "osc.gz");
    logger.info("Updating the database with the changeset: {}", changeUrl);

    // Process the changeset and update the database
    var buildNodeGeometry = new NodeGeometryBuilder();
    var reprojectNodeGeometry = new EntityProjectionTransformer(4326, databaseSrid);
    var prepareNodeGeometry =
        new ChangeEntitiesHandler(buildNodeGeometry.andThen(reprojectNodeGeometry));
    var importNodes = new ChangeElementsImporter<>(Node.class, nodeRepository);

    var buildWayGeometry = new WayGeometryBuilder(coordinateMap);
    var reprojectWayGeometry = new EntityProjectionTransformer(4326, databaseSrid);
    var prepareWayGeometry =
        new ChangeEntitiesHandler(buildWayGeometry.andThen(reprojectWayGeometry));
    var importWays = new ChangeElementsImporter<>(Way.class, wayRepository);

    var buildRelationGeometry = new RelationMultiPolygonBuilder(coordinateMap, referenceMap);
    var reprojectRelationGeometry = new EntityProjectionTransformer(4326, databaseSrid);
    var prepareRelationGeometry =
        new ChangeEntitiesHandler(buildRelationGeometry.andThen(reprojectRelationGeometry));
    var importRelations = new ChangeElementsImporter<>(Relation.class, relationRepository);

    var entityProcessor = prepareNodeGeometry
        .andThen(importNodes)
        .andThen(prepareWayGeometry)
        .andThen(importWays)
        .andThen(prepareRelationGeometry)
        .andThen(importRelations);

    try (var changeInputStream =
        new GZIPInputStream(new BufferedInputStream(changeUrl.openStream()))) {
      new XmlChangeReader().read(changeInputStream).forEach(entityProcessor);
    }

    // Add the new header to the database
    var stateUrl = stateReader.getUrl(nextSequenceNumber, "state.txt");
    try (var stateInputStream = new BufferedInputStream(stateUrl.openStream())) {
      var state = stateReader.read(stateInputStream);
      headerRepository.put(new Header(state.sequenceNumber(), state.timestamp(),
          header.replicationUrl(), header.source(), header.writingProgram()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new StringJoiner(", ", UpdateOsmDatabase.class.getSimpleName() + "[", "]")
        .add("database=" + database)
        .add("databaseSrid=" + databaseSrid)
        .add("replicationUrl='" + replicationUrl + "'")
        .toString();
  }
}
