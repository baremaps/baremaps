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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import daylight translations.
 */
public class ImportDaylightTranslations implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ImportDaylightTranslations.class);

  record Group(String type, Long id, String name) {

  }

  record Line(String type, Long id, String name, String attributeKey, String attributeValue) {

    public Group group() {
      return new Group(type, id, name);
    }

    public static Line parse(String line) {
      var parts = line.split("\t");
      var type = parts[0];
      var id = Long.parseLong(parts[1]);
      var name = parts[2];
      var key = parts[3];
      var val = parts[4];
      return new Line(type, id, name, key, val);
    }
  }

  private Path file;

  private Object database;

  /**
   * Constructs an {@code ImportDaylightTranslations}.
   */
  public ImportDaylightTranslations() {

  }

  /**
   * Constructs an {@code ImportDaylightTranslations}.
   *
   * @param file the daylight file
   * @param database the database
   */
  public ImportDaylightTranslations(Path file, Object database) {
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
    try (var lines = Files.lines(file)) {
      var entries = lines.map(Line::parse).collect(Collectors.groupingBy(Line::group));
      for (var entry : entries.entrySet()) {
        var key = entry.getKey();
        var value = entry.getValue();
        switch (entry.getKey().type()) {
          case "node" -> save(key, value, nodeRepository);
          case "way" -> save(key, value, wayRepository);
          case "relation" -> save(key, value, relationRepository);
          default -> logger.warn("Unknown type: {}", key.type());
        }
      }
    }
  }

  private <T extends Element> void save(Group group, List<Line> lines,
      Repository<Long, T> repository) throws RepositoryException {
    var entity = repository.get(group.id());
    if (entity != null) {
      var tags = new HashMap<>(entity.getTags());
      for (var line : lines) {
        tags.put(line.attributeKey(), line.attributeValue());
      }
      entity.setTags(tags);
      repository.put(entity);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new StringJoiner(", ", ImportDaylightTranslations.class.getSimpleName() + "[", "]")
        .add("file=" + file)
        .add("database=" + database)
        .toString();
  }
}
