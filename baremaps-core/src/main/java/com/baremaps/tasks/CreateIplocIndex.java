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

import com.baremaps.data.stream.StreamException;
import com.baremaps.iploc.IpLocReader;
import com.baremaps.iploc.IpLocRepository;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

public class CreateIplocIndex implements Task {

  private static final Logger logger = LoggerFactory.getLogger(CreateIplocIndex.class);

  private Path geonamesIndexPath;
  private List<Path> nicPaths;
  private Path targetIplocIndexPath;

  /**
   * Constructs a {@code CreateIplocIndex}.
   */
  public CreateIplocIndex() {

  }

  /**
   * Constructs a {@code CreateIplocIndex}.
   *
   * @param geonamesIndexPath the path to the geonames index
   * @param nicPaths the paths to the nic files
   * @param targetIplocIndexPath the path to the target iploc index
   */
  public CreateIplocIndex(Path geonamesIndexPath, List<Path> nicPaths, Path targetIplocIndexPath) {
    this.geonamesIndexPath = geonamesIndexPath;
    this.nicPaths = nicPaths;
    this.targetIplocIndexPath = targetIplocIndexPath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(WorkflowContext context) throws Exception {
    try (var directory = FSDirectory.open(geonamesIndexPath);
        var searcherManager = new SearcherManager(directory, new SearcherFactory())) {

      logger.info("Creating the Iploc database");
      var jdbcUrl = String.format("JDBC:sqlite:%s", targetIplocIndexPath);

      var config = new SQLiteConfig();
      var dataSource = new SQLiteDataSource(config);
      dataSource.setUrl(jdbcUrl);

      var ipLocRepository = new IpLocRepository(dataSource);
      ipLocRepository.dropTable();
      ipLocRepository.createTable();
      ipLocRepository.createIndex();

      var ipLocReader = new IpLocReader(searcherManager);
      nicPaths.parallelStream().forEach(path -> {
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
          var ipLocStream = ipLocReader.read(inputStream);
          ipLocRepository.save(ipLocStream);
        } catch (IOException e) {
          throw new StreamException(e);
        }
      });
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new StringJoiner(", ", CreateIplocIndex.class.getSimpleName() + "[", "]")
        .add("geonamesIndexPath=" + geonamesIndexPath)
        .add("nicPaths=" + nicPaths)
        .add("targetIplocIndexPath=" + targetIplocIndexPath)
        .toString();
  }
}
