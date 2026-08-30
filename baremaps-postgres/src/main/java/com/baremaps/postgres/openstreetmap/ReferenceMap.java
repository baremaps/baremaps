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

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

/**
 * A read-only map of the nodes referenced by the OpenStreetMap ways stored in Postgres, indexed by
 * way id.
 */
public class ReferenceMap extends PostgresMap<List<Long>> {

  /** Constructs a {@code ReferenceMap} over the default way table. */
  public ReferenceMap(DataSource dataSource) {
    this(dataSource, "public", "osm_way");
  }

  /** Constructs a {@code ReferenceMap} over the given way table. */
  public ReferenceMap(DataSource dataSource, String schema, String table) {
    super(dataSource, schema, table, "id", "nodes");
  }

  /** {@inheritDoc} */
  @Override
  protected List<Long> readValue(ResultSet row) throws SQLException {
    Array nodes = row.getArray(2);
    return nodes == null ? List.of() : Arrays.asList((Long[]) nodes.getArray());
  }
}
