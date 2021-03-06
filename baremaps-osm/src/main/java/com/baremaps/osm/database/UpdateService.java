package com.baremaps.osm.database;

import static com.baremaps.stream.ConsumerUtils.consumeThenReturn;

import com.baremaps.blob.BlobStore;
import com.baremaps.osm.OpenStreetMap;
import com.baremaps.osm.cache.Cache;
import com.baremaps.osm.domain.Change;
import com.baremaps.osm.domain.Entity;
import com.baremaps.osm.domain.Header;
import com.baremaps.osm.domain.State;
import com.baremaps.osm.geometry.CreateGeometryConsumer;
import com.baremaps.osm.geometry.ReprojectGeometryConsumer;
import com.baremaps.osm.handler.ChangeEntityConsumer;
import com.baremaps.osm.progress.InputStreamProgress;
import com.baremaps.osm.progress.ProgressLogger;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import org.locationtech.jts.geom.Coordinate;

public class UpdateService implements Callable<Void> {

  private final BlobStore blobStore;
  private final Cache<Long, Coordinate> coordinateCache;
  private final Cache<Long, List<Long>> referenceCache;
  private final HeaderTable headerTable;
  private final NodeTable nodeTable;
  private final WayTable wayTable;
  private final RelationTable relationTable;
  private final int srid;

  public UpdateService(
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
    Header header = headerTable.selectLatest();
    String replicationUrl = header.getReplicationUrl();
    Long sequenceNumber = header.getReplicationSequenceNumber() + 1;

    Consumer<Entity> createGeometry = new CreateGeometryConsumer(coordinateCache, referenceCache);
    Consumer<Entity> reprojectGeometry = new ReprojectGeometryConsumer(4326, srid);
    Consumer<Change> prepareGeometries = new ChangeEntityConsumer(createGeometry.andThen(reprojectGeometry));
    Function<Change, Change> prepareChange = consumeThenReturn(prepareGeometries);
    Consumer<Change> saveChange = new SaveChangeConsumer(nodeTable, wayTable, relationTable);

    URI changeUri = resolve(replicationUrl, sequenceNumber, "osc.gz");
    ProgressLogger progressLogger = new ProgressLogger(blobStore.size(changeUri), 5000);
    try (InputStream blobInputStream = blobStore.read(changeUri);
        InputStream progressInputStream = new InputStreamProgress(blobInputStream, progressLogger);
        InputStream gzipInputStream = new GZIPInputStream(progressInputStream)) {
      OpenStreetMap.streamXmlChanges(gzipInputStream).map(prepareChange).forEach(saveChange);
    }

    URI stateUri = resolve(replicationUrl, sequenceNumber, "state.txt");
    try (InputStream stateInputStream = blobStore.read(stateUri)) {
      State state = OpenStreetMap.readState(stateInputStream);
      headerTable.insert(new Header(
          state.getSequenceNumber(), state.getTimestamp(),
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

}
