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

import com.baremaps.data.geometry.GeometryUtils;
import com.baremaps.openstreetmap.model.Info;
import com.baremaps.openstreetmap.model.Way;
import com.baremaps.postgres.copy.CopyWriter;
import java.io.IOException;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

/** Stores OpenStreetMap ways in Postgres. */
public class WayRepository extends AbstractRepository<Way> {

  private static final List<Column> COLUMNS = List.of(
      Column.of("id", "int8"),
      Column.of("version", "int"),
      Column.of("uid", "int"),
      Column.of("timestamp", "timestamp without time zone"),
      Column.of("changeset", "int8"),
      Column.jsonb("tags"),
      Column.of("nodes", "int8[]"),
      Column.geometry("geom", "geometry"));

  /** Constructs a {@code WayRepository} over the default way table. */
  public WayRepository(DataSource dataSource) {
    this(dataSource, "public", "osm_way");
  }

  /** Constructs a {@code WayRepository} over the given way table. */
  public WayRepository(DataSource dataSource, String schema, String table) {
    super(dataSource, schema, table, COLUMNS);
  }

  /** {@inheritDoc} */
  @Override
  protected Way read(ResultSet row) throws SQLException, IOException {
    var info = new Info(
        row.getInt(2),
        row.getObject(4, LocalDateTime.class),
        row.getLong(5),
        row.getInt(3));
    Array nodes = row.getArray(7);
    return new Way(
        row.getLong(1),
        info,
        JsonbMapper.toMap(row.getString(6)),
        nodes == null ? List.of() : Arrays.asList((Long[]) nodes.getArray()),
        GeometryUtils.deserialize(row.getBytes(8)));
  }

  /** {@inheritDoc} */
  @Override
  protected void write(PreparedStatement statement, Way way) throws SQLException, IOException {
    statement.setObject(1, way.getId());
    statement.setObject(2, way.getInfo().version());
    statement.setObject(3, way.getInfo().uid());
    statement.setObject(4, way.getInfo().timestamp());
    statement.setObject(5, way.getInfo().changeset());
    statement.setObject(6, JsonbMapper.toJson(way.getTags()));
    statement.setArray(7,
        statement.getConnection().createArrayOf("int8", way.getNodes().toArray()));
    statement.setBytes(8, GeometryUtils.serialize(way.getGeometry()));
  }

  /** {@inheritDoc} */
  @Override
  protected void write(CopyWriter writer, Way way) throws IOException {
    writer.writeLong(way.getId());
    writer.writeInteger(way.getInfo().version());
    writer.writeInteger(way.getInfo().uid());
    writer.writeLocalDateTime(way.getInfo().timestamp());
    writer.writeLong(way.getInfo().changeset());
    writer.writeJsonb(JsonbMapper.toJson(way.getTags()));
    writer.writeLongList(way.getNodes());
    writer.writeGeometry(way.getGeometry());
  }
}
