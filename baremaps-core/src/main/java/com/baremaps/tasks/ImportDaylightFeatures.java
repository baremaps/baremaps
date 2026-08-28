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

import com.baremaps.openstreetmap.model.Element;
import com.baremaps.postgres.openstreetmap.*;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import daylight features.
 */
public class ImportDaylightFeatures implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ImportDaylightFeatures.class);

  record Feature(
      @JsonProperty("osm_type") String type,
      @JsonProperty("osm_id") Long id,
      @JsonProperty("tags") Map<String, Object> tags,
      @JsonProperty("wkt") String wkt,
      @JsonProperty("category") String category) {
  }

  private Path file;

  private Object database;

  /**
   * Constructs an {@code ImportDaylightFeatures}.
   */
  public ImportDaylightFeatures() {
    // Default constructor
  }

  /**
   * Constructs an {@code ImportDaylightFeatures}.
   *
   * @param file the daylight file
   * @param database the database
   */
  public ImportDaylightFeatures(Path file, Object database) {
    this.file = file;
    this.database = database;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(WorkflowContext context) throws Exception {
    var datasource = context.getDataSource(database);
    var nodeRepository = new NodeRepository(datasource);
    var wayRepository = new WayRepository(datasource);
    var relationRepository = new RelationRepository(datasource);

    // Initialize the repositories
    nodeRepository.create();
    wayRepository.create();
    relationRepository.create();

    // Process the file
    var objectMapper = new ObjectMapper();
    var javaType = objectMapper.getTypeFactory().constructParametricType(List.class, Feature.class);
    List<Feature> features = objectMapper.readValue(file.toFile(), javaType);
    for (var feature : features) {
      switch (feature.type()) {
        case "node" -> save(feature, nodeRepository);
        case "way" -> save(feature, wayRepository);
        case "relation" -> save(feature, relationRepository);
        default -> logger.warn("Unknown type: {}", feature.type());
      }
    }
  }

  private <T extends Element> void save(Feature feature, Repository<Long, T> repository)
      throws RepositoryException {
    var entity = repository.get(feature.id());
    if (entity != null) {
      var tags = new HashMap<>(feature.tags());
      if (entity.getTags() != null) {
        tags.putAll(entity.getTags());
      }
      @SuppressWarnings("unchecked")
      var updated = (T) entity.withTags(tags);
      repository.put(updated);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new StringJoiner(", ", ImportDaylightFeatures.class.getSimpleName() + "[", "]")
        .add("file=" + file)
        .add("database=" + database)
        .toString();
  }
}
