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

package com.baremaps.postgres.openstreetmap;

import java.util.List;
import javax.sql.DataSource;

/**
 * The four tables an OpenStreetMap import writes to: headers, nodes, ways and relations.
 *
 * <p>
 * They are created, dropped and emptied together because they are one dataset: a node table without
 * the ways that reference it is not a state any task wants. Handling them as a group is also what
 * keeps the repositories to a single purpose, reading and writing entities.
 */
public final class OsmSchema {

  private final List<AbstractRepository<?>> repositories;

  /** Constructs an {@code OsmSchema} over the default tables. */
  public OsmSchema(DataSource dataSource) {
    this(dataSource, "public");
  }

  /** Constructs an {@code OsmSchema} over the tables of the given schema. */
  public OsmSchema(DataSource dataSource, String schema) {
    this.repositories = List.of(
        new HeaderRepository(dataSource, schema, "osm_header"),
        new NodeRepository(dataSource, schema, "osm_node"),
        new WayRepository(dataSource, schema, "osm_way"),
        new RelationRepository(dataSource, schema, "osm_relation"));
  }

  /** Creates the tables that do not exist yet, leaving the others as they are. */
  public void create() throws RepositoryException {
    for (var repository : repositories) {
      repository.createTable();
    }
  }

  /** Drops the tables, and everything that depends on them. */
  public void drop() throws RepositoryException {
    for (var repository : repositories) {
      repository.dropTable();
    }
  }

  /** Removes every row of the tables. */
  public void truncate() throws RepositoryException {
    for (var repository : repositories) {
      repository.truncateTable();
    }
  }
}
