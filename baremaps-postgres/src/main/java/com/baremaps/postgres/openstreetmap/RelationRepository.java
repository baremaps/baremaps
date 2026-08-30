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
import com.baremaps.openstreetmap.model.Member;
import com.baremaps.openstreetmap.model.Member.MemberType;
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.postgres.copy.CopyWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 * Stores OpenStreetMap relations in Postgres.
 *
 * <p>
 * A member has a reference, a type and a role. Postgres has no array of records, so the members are
 * stored as three parallel arrays, which the mappers below zip back together.
 */
public class RelationRepository extends AbstractRepository<Relation> {

  private static final List<Column> COLUMNS = List.of(
      Column.of("id", "int8"),
      Column.of("version", "int"),
      Column.of("uid", "int"),
      Column.of("timestamp", "timestamp without time zone"),
      Column.of("changeset", "int8"),
      Column.jsonb("tags"),
      Column.of("member_refs", "int8[]"),
      Column.of("member_types", "int[]"),
      Column.of("member_roles", "text[]"),
      Column.geometry("geom", "geometry"));

  /** Constructs a {@code RelationRepository} over the default relation table. */
  public RelationRepository(DataSource dataSource) {
    this(dataSource, "public", "osm_relation");
  }

  /** Constructs a {@code RelationRepository} over the given relation table. */
  public RelationRepository(DataSource dataSource, String schema, String table) {
    super(dataSource, schema, table, COLUMNS);
  }

  /** {@inheritDoc} */
  @Override
  protected Relation read(ResultSet row) throws SQLException, IOException {
    var info = new Info(
        row.getInt(2),
        row.getObject(4, LocalDateTime.class),
        row.getLong(5),
        row.getInt(3));
    return new Relation(
        row.getLong(1),
        info,
        JsonbMapper.toMap(row.getString(6)),
        readMembers(row),
        GeometryUtils.deserialize(row.getBytes(10)));
  }

  private static List<Member> readMembers(ResultSet row) throws SQLException {
    var refs = row.getArray(7);
    var types = row.getArray(8);
    var roles = row.getArray(9);
    if (refs == null || types == null || roles == null) {
      return List.of();
    }
    var refValues = (Long[]) refs.getArray();
    var typeValues = (Integer[]) types.getArray();
    var roleValues = (String[]) roles.getArray();
    var members = new ArrayList<Member>(refValues.length);
    for (int index = 0; index < refValues.length; index++) {
      members.add(new Member(refValues[index], MemberType.forNumber(typeValues[index]),
          roleValues[index]));
    }
    return members;
  }

  /** {@inheritDoc} */
  @Override
  protected void write(PreparedStatement statement, Relation relation)
      throws SQLException, IOException {
    var connection = statement.getConnection();
    statement.setObject(1, relation.getId());
    statement.setObject(2, relation.getInfo().version());
    statement.setObject(3, relation.getInfo().uid());
    statement.setObject(4, relation.getInfo().timestamp());
    statement.setObject(5, relation.getInfo().changeset());
    statement.setObject(6, JsonbMapper.toJson(relation.getTags()));
    statement.setArray(7, connection.createArrayOf("int8", refs(relation).toArray()));
    statement.setArray(8, connection.createArrayOf("int", types(relation).toArray()));
    statement.setArray(9, connection.createArrayOf("text", roles(relation).toArray()));
    statement.setBytes(10, GeometryUtils.serialize(relation.getGeometry()));
  }

  /** {@inheritDoc} */
  @Override
  protected void write(CopyWriter writer, Relation relation) throws IOException {
    writer.writeLong(relation.getId());
    writer.writeInteger(relation.getInfo().version());
    writer.writeInteger(relation.getInfo().uid());
    writer.writeLocalDateTime(relation.getInfo().timestamp());
    writer.writeLong(relation.getInfo().changeset());
    writer.writeJsonb(JsonbMapper.toJson(relation.getTags()));
    writer.writeLongList(refs(relation));
    writer.writeIntegerList(types(relation));
    writer.write(roles(relation));
    writer.writeGeometry(relation.getGeometry());
  }

  private static List<Long> refs(Relation relation) {
    return relation.getMembers().stream().map(Member::ref).toList();
  }

  private static List<Integer> types(Relation relation) {
    return relation.getMembers().stream().map(Member::type).map(MemberType::ordinal).toList();
  }

  private static List<String> roles(Relation relation) {
    return relation.getMembers().stream().map(Member::role).toList();
  }
}
