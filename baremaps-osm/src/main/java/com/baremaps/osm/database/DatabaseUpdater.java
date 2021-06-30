package com.baremaps.osm.database;

import com.baremaps.blob.BlobStore;
import com.baremaps.osm.OpenStreetMap;
import com.baremaps.osm.cache.Cache;
import com.baremaps.osm.domain.Change;
import com.baremaps.osm.domain.Element;
import com.baremaps.osm.domain.Header;
import com.baremaps.osm.domain.Node;
import com.baremaps.osm.domain.Relation;
import com.baremaps.osm.domain.State;
import com.baremaps.osm.domain.Way;
import com.baremaps.osm.geometry.GeometryHandler;
import com.baremaps.osm.geometry.ProjectionTransformer;
import com.baremaps.osm.handler.ChangeHandler;
import com.baremaps.osm.handler.ElementHandler;
import com.baremaps.osm.progress.InputStreamProgress;
import com.baremaps.osm.progress.ProgressLogger;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseUpdater implements Callable<Void> {

  private static Logger logger = LoggerFactory.getLogger(DatabaseUpdater.class);

  private final BlobStore blobStore;
  private final Cache<Long, Coordinate> coordinateCache;
  private final Cache<Long, List<Long>> referenceCache;
  private final HeaderTable headerTable;
  private final NodeTable nodeTable;
  private final WayTable wayTable;
  private final RelationTable relationTable;
  private final int srid;

  public DatabaseUpdater(
      BlobStore blobStore,
      Cache<Long, Coordinate> coordinateCache,
      Cache<Long, List<Long>> referenceCache,
      HeaderTable headerTable,
      NodeTable nodeTable,
      WayTable wayTable,
      RelationTable relationTable,
      int srid) {
    this.blobStore = blobStore;
    this.coordinateCache = coordinateCache;
    this.referenceCache = referenceCache;
    this.headerTable = headerTable;
    this.nodeTable = nodeTable;
    this.wayTable = wayTable;
    this.relationTable = relationTable;
    this.srid = srid;
  }

  @Override
  public Void call() throws Exception {
    Header header = headerTable.latest();
    String replicationUrl = header.getReplicationUrl();
    Long sequenceNumber = header.getReplicationSequenceNumber() + 1;

    GeometryHandler geometryHandler = new GeometryHandler(coordinateCache, referenceCache);
    ProjectionTransformer projectionTransformer = new ProjectionTransformer(4326, srid);
    UpdateHandler updateHandler = new UpdateHandler();
    URI changeUri = resolve(replicationUrl, sequenceNumber, "osc.gz");
    ProgressLogger progressLogger = new ProgressLogger(blobStore.size(changeUri), 5000);
    try (InputStream changesInputStream = new GZIPInputStream(new InputStreamProgress(blobStore.read(changeUri), progressLogger))) {
      OpenStreetMap.streamXmlChanges(changesInputStream)
          .peek(change -> change.getElements().forEach(geometryHandler.andThen(projectionTransformer)))
          .forEach(updateHandler);
    }

    URI stateUri = resolve(replicationUrl, sequenceNumber, "state.txt");
    try (InputStream stateInputStream = blobStore.read(stateUri)) {
      State state = OpenStreetMap.readState(stateInputStream);
      headerTable.insert(new Header(
          state.getTimestamp(),
          state.getSequenceNumber(),
          header.getReplicationUrl(),
          header.getSource(),
          header.getWritingProgram()));
    }

    return null;
  }

  public URI resolve(String replicationUrl, Long sequenceNumber, String extension) throws URISyntaxException {
    String s = String.format("%09d", sequenceNumber);
    return new URI(String.format("%s/%s/%s/%s.%s",
        replicationUrl,
        s.substring(0, 3),
        s.substring(3, 6),
        s.substring(6, 9),
        extension));
  }

  private class UpdateHandler implements ChangeHandler {

    @Override
    public void handle(Change change) throws Exception {
      for (Element element : change.getElements()) {
        element.accept(new ElementHandler() {
          @Override
          public void handle(Node node) throws Exception {
            switch (change.getType()) {
              case create:
              case modify:
                nodeTable.insert(node);
                break;
              case delete:
                nodeTable.delete(node.getId());
                break;
            }
          }

          @Override
          public void handle(Way way) throws Exception {
            switch (change.getType()) {
              case create:
              case modify:
                wayTable.insert(way);
                break;
              case delete:
                wayTable.delete(way.getId());
                break;
            }
          }

          @Override
          public void handle(Relation relation) throws Exception {
            switch (change.getType()) {
              case create:
              case modify:
                relationTable.insert(relation);
                break;
              case delete:
                relationTable.delete(relation.getId());
                break;
            }
          }
        });
      }
    }
  }
}
