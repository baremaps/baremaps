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

package com.baremaps.workflow;


import com.baremaps.data.collection.AppendOnlyLog;
import com.baremaps.data.collection.DataConversions;
import com.baremaps.data.collection.IndexedDataList;
import com.baremaps.data.collection.MemoryAlignedDataList;
import com.baremaps.data.collection.MonotonicDataMap;
import com.baremaps.data.memory.MemoryMappedDirectory;
import com.baremaps.data.type.LonLatDataType;
import com.baremaps.data.type.LongDataType;
import com.baremaps.data.type.LongListDataType;
import com.baremaps.data.util.FileUtils;
import com.baremaps.postgres.utils.PostgresUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.locationtech.jts.geom.Coordinate;

/**
 * A context that is passed to the tasks of a workflow and used to share data between tasks.
 */
public class WorkflowContext {

  private final Path dataDir;

  private final Path cacheDir;

  public WorkflowContext() {
    this(Paths.get("./data"), Paths.get("./cache"));
  }

  public WorkflowContext(Path dataDir, Path cacheDir) {
    this.dataDir = dataDir;
    this.cacheDir = cacheDir;
  }

  private Map<Object, DataSource> dataSources = new ConcurrentHashMap<>();

  /**
   * Returns the data source associated with the specified database.
   *
   * @param database the JDBC connection string to the database
   * @return the data source
   */
  public DataSource getDataSource(Object database) {
    return dataSources.computeIfAbsent(database, PostgresUtils::createDataSourceFromObject);
  }

  /**
   * Returns a map from node ids to coordinates, backed by the cache directory. Coordinates are
   * quantized to 8 bytes, which keeps a planet's worth of nodes within a few tens of gigabytes.
   */
  public Map<Long, Coordinate> getCoordinateMap() throws IOException {
    Path dir = cacheDir.resolve("coordinates");
    return DataConversions.asMap(new MonotonicDataMap<>(
        new MemoryAlignedDataList<>(new LongDataType(),
            new MemoryMappedDirectory(dir.resolve("offsets"))),
        new MemoryAlignedDataList<>(new LongDataType(),
            new MemoryMappedDirectory(dir.resolve("keys"))),
        new MemoryAlignedDataList<>(new LonLatDataType(),
            new MemoryMappedDirectory(dir.resolve("values")))));
  }

  /** Returns a map from way ids to their node ids, backed by the cache directory. */
  public Map<Long, List<Long>> getReferenceMap() throws IOException {
    Path dir = cacheDir.resolve("references");
    return DataConversions.asMap(new MonotonicDataMap<>(
        new MemoryAlignedDataList<>(new LongDataType(),
            new MemoryMappedDirectory(dir.resolve("offsets"))),
        new MemoryAlignedDataList<>(new LongDataType(),
            new MemoryMappedDirectory(dir.resolve("keys"))),
        new IndexedDataList<>(
            new MemoryAlignedDataList<>(new LongDataType(),
                new MemoryMappedDirectory(dir.resolve("index"))),
            new AppendOnlyLog<>(new LongListDataType(),
                new MemoryMappedDirectory(dir.resolve("values"))))));
  }

  public void cleanCache() throws IOException {
    FileUtils.deleteRecursively(cacheDir);
  }

  public void cleanData() throws IOException {
    FileUtils.deleteRecursively(dataDir);
  }
}
