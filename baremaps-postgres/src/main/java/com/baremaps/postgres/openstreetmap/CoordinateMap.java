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

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.locationtech.jts.geom.Coordinate;

/**
 * A read-only map of the coordinates of the OpenStreetMap nodes stored in Postgres, indexed by node
 * id.
 */
public class CoordinateMap extends PostgresMap<Coordinate> {

  /** Constructs a {@code CoordinateMap} over the default node table. */
  public CoordinateMap(DataSource dataSource) {
    this(dataSource, "public", "osm_node");
  }

  /** Constructs a {@code CoordinateMap} over the given node table. */
  public CoordinateMap(DataSource dataSource, String schema, String table) {
    super(dataSource, schema, table, "id", "lon", "lat");
  }

  /** {@inheritDoc} */
  @Override
  protected Coordinate readValue(ResultSet row) throws SQLException {
    return new Coordinate(row.getDouble(2), row.getDouble(3));
  }
}
