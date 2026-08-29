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
import com.baremaps.data.collection.CloseableMap;
import com.baremaps.data.collection.DataConversions;
import com.baremaps.data.collection.DenseDataMap;
import com.baremaps.data.collection.VariableSizeDataMap;
import com.baremaps.data.memory.Memory;
import com.baremaps.data.type.FixedSizeDataType;
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
   * quantized to 8 bytes and the ids of a planet are dense, so a {@link DenseDataMap} keeps a
   * planet's worth of nodes within a few tens of gigabytes at one probe per lookup.
   *
   * <p>
   * The map is persistent: close it and the next call reopens it with its content, which is how an
   * update reuses the index built by an import instead of querying the database.
   */
  public CloseableMap<Long, Coordinate> getCoordinateMap() throws IOException {
    return DataConversions.asMap(denseMap(new LonLatDataType(), cacheDir.resolve("coordinates")));
  }

  /** Returns a map from way ids to their node ids, backed by the cache directory. */
  public CloseableMap<Long, List<Long>> getReferenceMap() throws IOException {
    Path dir = cacheDir.resolve("references");
    return DataConversions.asMap(new VariableSizeDataMap<>(
        denseMap(new LongDataType(), dir),
        new AppendOnlyLog<>(new LongListDataType(),
            Memory.mappedDirectory(dir.resolve("values")))));
  }

  private static <E> DenseDataMap<E> denseMap(FixedSizeDataType<E> dataType, Path dir) {
    return new DenseDataMap<>(dataType, DenseDataMap.DEFAULT_PAGE_SHIFT,
        Memory.mappedDirectory(dir.resolve("table")),
        Memory.mappedDirectory(dir.resolve("presence")),
        Memory.mappedDirectory(dir.resolve("pages")));
  }

  public void cleanCache() throws IOException {
    FileUtils.deleteRecursively(cacheDir);
  }

  public void cleanData() throws IOException {
    FileUtils.deleteRecursively(dataDir);
  }
}
