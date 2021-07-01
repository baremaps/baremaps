package com.baremaps.osm.database;

import com.baremaps.blob.BlobStore;
import com.baremaps.osm.OpenStreetMap;
import com.baremaps.osm.cache.Cache;
import com.baremaps.osm.cache.CacheImporter;
import com.baremaps.osm.domain.Block;
import com.baremaps.osm.domain.DataBlock;
import com.baremaps.osm.domain.HeaderBlock;
import com.baremaps.osm.geometry.GeometryConsumer;
import com.baremaps.osm.geometry.ProjectionConsumer;
import com.baremaps.osm.handler.BlockEntityConsumer;
import com.baremaps.osm.handler.BlockConsumerAdapter;
import com.baremaps.osm.progress.InputStreamProgress;
import com.baremaps.osm.progress.ProgressLogger;
import com.baremaps.stream.StreamUtils;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseImporter implements Callable<Void> {

  private static Logger logger = LoggerFactory.getLogger(DatabaseImporter.class);

  private final URI file;
  private final BlobStore blobStore;
  private final Cache<Long, Coordinate> coordinateCache;
  private final Cache<Long, List<Long>> referenceCache;
  private final HeaderTable headerTable;
  private final NodeTable nodeTable;
  private final WayTable wayTable;
  private final RelationTable relationTable;
  private final int srid;

  public DatabaseImporter(
      URI file,
      BlobStore blobStore,
      Cache<Long, Coordinate> coordinateCache,
      Cache<Long, List<Long>> referenceCache,
      HeaderTable headerTable,
      NodeTable nodeTable,
      WayTable wayTable,
      RelationTable relationTable,
      int srid) {
    this.file = file;
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
    logger.info("Importing data");

    CacheImporter cacheImporter = new CacheImporter(coordinateCache, referenceCache);
    ImportConsumer importHandler = new ImportConsumer();
    GeometryConsumer geometryFetcher = new GeometryConsumer(coordinateCache, referenceCache);
    ProjectionConsumer projectionConsumer = new ProjectionConsumer(4326, srid);
    BlockEntityConsumer geometryFactory = new BlockEntityConsumer(geometryFetcher.andThen(projectionConsumer));
    Consumer<Block> blockHandler = cacheImporter.andThen(geometryFactory);

    ProgressLogger progressLogger = new ProgressLogger(blobStore.size(file), 5000);
    try (InputStream inputStream = new InputStreamProgress(blobStore.read(this.file), progressLogger)) {
      Stream<Block> stream = OpenStreetMap.streamPbfBlocks(inputStream).peek(blockHandler);
      StreamUtils.batch(stream).forEach(importHandler);
    }

    return null;
  }

  private class ImportConsumer implements BlockConsumerAdapter {

    @Override
    public void match(HeaderBlock headerBlock) throws Exception {
      headerTable.insert(headerBlock.getHeader());
    }

    @Override
    public void match(DataBlock dataBlock) throws Exception {
      nodeTable.copy(dataBlock.getDenseNodes());
      nodeTable.copy(dataBlock.getNodes());
      wayTable.copy(dataBlock.getWays());
      relationTable.copy(dataBlock.getRelations());
    }
  }

}
