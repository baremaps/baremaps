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


import com.baremaps.data.collection.*;
import com.baremaps.data.memory.MemoryMappedDirectory;
import com.baremaps.data.type.*;
import com.baremaps.data.util.FileUtils;
import com.baremaps.postgres.utils.PostgresUtils;
import java.io.IOException;
import java.nio.file.Files;
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

  public Map<Long, Coordinate> getCoordinateMap() throws IOException {
    return DataConversions.asMap(getMonotonicPairedDataMap("coordinates", new LonLatDataType()));
  }

  public Map<Long, List<Long>> getReferenceMap() throws IOException {
    return DataConversions.asMap(getMonotonicDataMap("references", new LongListDataType()));
  }

  private <T> DataMap<Long, T> getMemoryAlignedDataMap(String name, FixedSizeDataType<T> dataType)
      throws IOException {
    Path coordinateDir = Files.createDirectories(cacheDir.resolve(name));
    return MemoryAlignedDataMap.<T>builder()
        .dataType(dataType)
        .memory(new MemoryMappedDirectory(coordinateDir))
        .build();
  }

  private <T> DataMap<Long, T> getMonotonicDataMap(String name, DataType<T> dataType)
      throws IOException {
    Path mapDir = Files.createDirectories(cacheDir.resolve(name));
    Path keysDir = Files.createDirectories(mapDir.resolve("keys"));
    Path valuesDir = Files.createDirectories(mapDir.resolve("values"));
    MemoryAlignedDataList<PairDataType.Pair<Long, Long>> keys =
        MemoryAlignedDataList.<PairDataType.Pair<Long, Long>>builder()
            .dataType(new PairDataType<>(new LongDataType(), new LongDataType()))
            .memory(new MemoryMappedDirectory(keysDir))
            .build();
    AppendOnlyLog<T> values = AppendOnlyLog.<T>builder()
        .dataType(dataType)
        .values(new MemoryMappedDirectory(valuesDir))
        .build();
    return MonotonicDataMap.<T>builder()
        .keys(keys)
        .values(values)
        .build();
  }

  private DataMap<Long, Coordinate> getMonotonicPairedDataMap(String name,
      DataType<Coordinate> dataType)
      throws IOException {
    Path mapDir = Files.createDirectories(cacheDir.resolve(name));
    return MonotonicPairedDataMap.<Coordinate>builder()
        .values(MemoryAlignedDataList.<PairDataType.Pair<Long, Coordinate>>builder()
            .dataType(new PairDataType<>(new LongDataType(), new LonLatDataType()))
            .memory(new MemoryMappedDirectory(Files.createDirectories(mapDir)))
            .build())
        .build();
  }

  public void cleanCache() throws IOException {
    FileUtils.deleteRecursively(cacheDir);
  }

  public void cleanData() throws IOException {
    FileUtils.deleteRecursively(dataDir);
  }
}
