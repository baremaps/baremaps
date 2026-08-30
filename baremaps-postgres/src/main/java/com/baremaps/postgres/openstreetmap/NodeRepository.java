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
import com.baremaps.openstreetmap.model.Node;
import com.baremaps.postgres.copy.CopyWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;

/** Stores OpenStreetMap nodes in Postgres. */
public class NodeRepository extends AbstractRepository<Node> {

  private static final List<Column> COLUMNS = List.of(
      Column.of("id", "int8"),
      Column.of("version", "int"),
      Column.of("uid", "int"),
      Column.of("timestamp", "timestamp without time zone"),
      Column.of("changeset", "int8"),
      Column.jsonb("tags"),
      Column.of("lon", "float"),
      Column.of("lat", "float"),
      Column.geometry("geom", "geometry(point)"));

  /** Constructs a {@code NodeRepository} over the default node table. */
  public NodeRepository(DataSource dataSource) {
    this(dataSource, "public", "osm_node");
  }

  /** Constructs a {@code NodeRepository} over the given node table. */
  public NodeRepository(DataSource dataSource, String schema, String table) {
    super(dataSource, schema, table, COLUMNS);
  }

  /** {@inheritDoc} */
  @Override
  protected Node read(ResultSet row) throws SQLException, IOException {
    var info = new Info(
        row.getInt(2),
        row.getObject(4, LocalDateTime.class),
        row.getLong(5),
        row.getInt(3));
    return new Node(
        row.getLong(1),
        info,
        JsonbMapper.toMap(row.getString(6)),
        row.getDouble(7),
        row.getDouble(8),
        GeometryUtils.deserialize(row.getBytes(9)));
  }

  /** {@inheritDoc} */
  @Override
  protected void write(PreparedStatement statement, Node node) throws SQLException, IOException {
    statement.setObject(1, node.getId());
    statement.setObject(2, node.getInfo().version());
    statement.setObject(3, node.getInfo().uid());
    statement.setObject(4, node.getInfo().timestamp());
    statement.setObject(5, node.getInfo().changeset());
    statement.setObject(6, JsonbMapper.toJson(node.getTags()));
    statement.setObject(7, node.getLon());
    statement.setObject(8, node.getLat());
    statement.setBytes(9, GeometryUtils.serialize(node.getGeometry()));
  }

  /** {@inheritDoc} */
  @Override
  protected void write(CopyWriter writer, Node node) throws IOException {
    writer.writeLong(node.getId());
    writer.writeInteger(node.getInfo().version());
    writer.writeInteger(node.getInfo().uid());
    writer.writeLocalDateTime(node.getInfo().timestamp());
    writer.writeLong(node.getInfo().changeset());
    writer.writeJsonb(JsonbMapper.toJson(node.getTags()));
    writer.writeDouble(node.getLon());
    writer.writeDouble(node.getLat());
    writer.writeGeometry(node.getGeometry());
  }
}
