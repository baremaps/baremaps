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

import com.baremaps.openstreetmap.model.Info;
import com.baremaps.openstreetmap.model.Member;
import com.baremaps.openstreetmap.model.Member.MemberType;
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.openstreetmap.utils.GeometryUtils;
import com.baremaps.postgres.copy.CopyWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import javax.sql.DataSource;
import org.locationtech.jts.geom.Geometry;
import org.postgresql.PGConnection;
import org.postgresql.copy.PGCopyOutputStream;

/** Provides an implementation of the {@code RelationRepository} baked by Postgres. */
public class RelationRepository implements Repository<Long, Relation> {

  private final DataSource dataSource;

  private final String createTable;

  private final String dropTable;

  private final String truncateTable;

  private final String select;

  private final String selectIn;

  private final String insert;

  private final String delete;

  private final String deleteIn;

  private final String copy;

  /**
   * Constructs a {@code PostgresRelationRepository}.
   *
   * @param dataSource
   */
  public RelationRepository(DataSource dataSource) {
    this(
        dataSource,
        "public",
        "osm_relation",
        "id",
        "version",
        "uid",
        "timestamp",
        "changeset",
        "tags",
        "member_refs",
        "member_types",
        "member_roles",
        "geom");
  }

  /**
   * Constructs a {@code PostgresRelationRepository} with custom parameters.
   *
   * @param dataSource
   * @param schema
   * @param table
   * @param idColumn
   * @param versionColumn
   * @param uidColumn
   * @param timestampColumn
   * @param changesetColumn
   * @param tagsColumn
   * @param memberRefs
   * @param memberTypes
   * @param memberRoles
   * @param geometryColumn
   */
  @SuppressWarnings("squid:S107")
  public RelationRepository(
      DataSource dataSource,
      String schema,
      String table,
      String idColumn,
      String versionColumn,
      String uidColumn,
      String timestampColumn,
      String changesetColumn,
      String tagsColumn,
      String memberRefs,
      String memberTypes,
      String memberRoles,
      String geometryColumn) {
    var fullTableName = String.format("%1$s.%2$s", schema, table);
    this.dataSource = dataSource;
    this.createTable = String.format("""
        CREATE TABLE IF NOT EXISTS %1$s (
          %2$s bigint PRIMARY KEY,
          %3$s int,
          %4$s int,
          %5$s timestamp without time zone,
          %6$s bigint,
          %7$s jsonb,
          %8$s bigint[],
          %9$s int[],
          %10$s text[],
          %11$s geometry
        )""", fullTableName, idColumn, versionColumn, uidColumn, timestampColumn, changesetColumn,
        tagsColumn, memberRefs, memberTypes, memberRoles, geometryColumn);
    this.dropTable = String.format("DROP TABLE IF EXISTS %1$s CASCADE", fullTableName);
    this.truncateTable = String.format("TRUNCATE TABLE %1$s", fullTableName);
    this.select = String.format(
        "SELECT %2$s, %3$s, %4$s, %5$s, %6$s, %7$s, %8$s, %9$s, %10$s, st_asewkb(%11$s) FROM %1$s WHERE %2$s = ?",
        fullTableName, idColumn, versionColumn, uidColumn, timestampColumn, changesetColumn,
        tagsColumn,
        memberRefs, memberTypes, memberRoles, geometryColumn);
    this.selectIn = String.format(
        "SELECT %2$s, %3$s, %4$s, %5$s, %6$s, %7$s, %8$s, %9$s, %10$s, st_asewkb(%11$s) FROM %1$s WHERE %2$s = ANY (?)",
        fullTableName, idColumn, versionColumn, uidColumn, timestampColumn, changesetColumn,
        tagsColumn,
        memberRefs, memberTypes, memberRoles, geometryColumn);
    this.insert = String.format("""
        INSERT INTO %1$s (%2$s, %3$s, %4$s, %5$s, %6$s, %7$s, %8$s, %9$s, %10$s, %11$s)
        VALUES (?, ?, ?, ?, ?, cast (? AS jsonb), ?, ?, ?, ?)
        ON CONFLICT (%2$s) DO UPDATE SET
        %3$s = excluded.%3$s,
        %4$s = excluded.%4$s,
        %5$s = excluded.%5$s,
        %6$s = excluded.%6$s,
        %7$s = excluded.%7$s,
        %8$s = excluded.%8$s,
        %9$s = excluded.%9$s,
        %10$s = excluded.%10$s,
        %11$s = excluded.%11$s""", fullTableName, idColumn, versionColumn, uidColumn,
        timestampColumn,
        changesetColumn, tagsColumn, memberRefs, memberTypes, memberRoles, geometryColumn);
    this.delete = String.format("DELETE FROM %1$s WHERE %2$s = ?", fullTableName, idColumn);
    this.deleteIn = String.format("DELETE FROM %1$s WHERE %2$s = ANY (?)", fullTableName, idColumn);
    this.copy = String.format(
        "COPY %1$s (%2$s, %3$s, %4$s, %5$s, %6$s, %7$s, %8$s, %9$s, %10$s, %11$s) FROM STDIN BINARY",
        fullTableName, idColumn, versionColumn, uidColumn, timestampColumn, changesetColumn,
        tagsColumn,
        memberRefs, memberTypes, memberRoles, geometryColumn);
  }

  /** {@inheritDoc} */
  @Override
  public void create() throws RepositoryException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(createTable)) {
      statement.execute();
    } catch (SQLException e) {
      throw new RepositoryException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void drop() throws RepositoryException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(dropTable)) {
      statement.execute();
    } catch (SQLException e) {
      throw new RepositoryException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void truncate() throws RepositoryException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(truncateTable)) {
      statement.execute();
    } catch (SQLException e) {
      throw new RepositoryException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Relation get(Long key) throws RepositoryException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(select)) {
      statement.setObject(1, key);
      try (ResultSet result = statement.executeQuery()) {
        if (result.next()) {
          return getValue(result);
        } else {
          return null;
        }
      }
    } catch (SQLException | JsonProcessingException e) {
      throw new RepositoryException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Relation> get(List<Long> keys) throws RepositoryException {
    if (keys.isEmpty()) {
      return List.of();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectIn)) {
      statement.setArray(1, connection.createArrayOf("int8", keys.toArray()));
      try (ResultSet result = statement.executeQuery()) {
        Map<Long, Relation> values = new HashMap<>();
        while (result.next()) {
          Relation value = getValue(result);
          values.put(value.getId(), value);
        }
        return keys.stream().map(values::get).toList();
      }
    } catch (SQLException | JsonProcessingException e) {
      throw new RepositoryException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void put(Relation value) throws RepositoryException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(insert)) {
      setValue(statement, value);
      statement.execute();
    } catch (SQLException | JsonProcessingException e) {
      throw new RepositoryException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void put(List<Relation> values) throws RepositoryException {
    if (values.isEmpty()) {
      return;
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(insert)) {
      for (Relation value : values) {
        statement.clearParameters();
        setValue(statement, value);
        statement.addBatch();
      }
      statement.executeBatch();
    } catch (SQLException | JsonProcessingException e) {
      throw new RepositoryException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(Long key) throws RepositoryException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(delete)) {
      statement.setObject(1, key);
      statement.execute();
    } catch (SQLException e) {
      throw new RepositoryException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(List<Long> keys) throws RepositoryException {
    if (keys.isEmpty()) {
      return;
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(deleteIn)) {
      statement.setArray(1, connection.createArrayOf("int8", keys.toArray()));
      statement.execute();
    } catch (SQLException e) {
      throw new RepositoryException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void copy(List<Relation> values) throws RepositoryException {
    if (values.isEmpty()) {
      return;
    }
    try (Connection connection = dataSource.getConnection()) {
      PGConnection pgConnection = connection.unwrap(PGConnection.class);
      try (CopyWriter writer = new CopyWriter(new PGCopyOutputStream(pgConnection, copy))) {
        writer.writeHeader();
        for (Relation value : values) {
          writer.startRow(10);
          writer.writeLong(value.getId());
          writer.writeInteger(value.getInfo().getVersion());
          writer.writeInteger(value.getInfo().getUid());
          writer.writeLocalDateTime(value.getInfo().getTimestamp());
          writer.writeLong(value.getInfo().getChangeset());
          writer.writeJsonb(JsonbMapper.toJson(value.getTags()));
          writer.writeLongList(
              value.getMembers().stream().map(Member::ref).toList());
          writer.writeIntegerList(value.getMembers().stream().map(Member::type)
              .map(MemberType::ordinal).toList());
          writer
              .write(value.getMembers().stream().map(Member::role).toList());
          writer.writeGeometry(value.getGeometry());
        }
      }
    } catch (IOException | SQLException ex) {
      throw new RepositoryException(ex);
    }
  }

  private Relation getValue(ResultSet resultSet) throws SQLException, JsonProcessingException {
    long id = resultSet.getLong(1);
    int version = resultSet.getInt(2);
    int uid = resultSet.getInt(3);
    LocalDateTime timestamp = resultSet.getObject(4, LocalDateTime.class);
    long changeset = resultSet.getLong(5);
    Map<String, Object> tags = JsonbMapper.toMap(resultSet.getString(6));
    Long[] refs = (Long[]) resultSet.getArray(7).getArray();
    Integer[] types = (Integer[]) resultSet.getArray(8).getArray();
    String[] roles = (String[]) resultSet.getArray(9).getArray();
    List<Member> members = new ArrayList<>();
    for (int i = 0; i < refs.length; i++) {
      members.add(new Member(refs[i], MemberType.forNumber(types[i]), roles[i]));
    }
    Geometry geometry = GeometryUtils.deserialize(resultSet.getBytes(10));
    Info info = new Info(version, timestamp, changeset, uid);
    return new Relation(id, info, tags, members, geometry);
  }

  private void setValue(PreparedStatement statement, Relation value)
      throws SQLException, JsonProcessingException {
    statement.setObject(1, value.getId());
    statement.setObject(2, value.getInfo().getVersion());
    statement.setObject(3, value.getInfo().getUid());
    statement.setObject(4, value.getInfo().getTimestamp());
    statement.setObject(5, value.getInfo().getChangeset());
    statement.setObject(6, JsonbMapper.toJson(value.getTags()));
    Object[] refs = value.getMembers().stream().map(Member::ref).toArray();
    statement.setObject(7, statement.getConnection().createArrayOf("bigint", refs));
    Object[] types =
        value.getMembers().stream().map(Member::type).map(MemberType::ordinal).toArray();
    statement.setObject(8, statement.getConnection().createArrayOf("int", types));
    Object[] roles = value.getMembers().stream().map(Member::role).toArray();
    statement.setObject(9, statement.getConnection().createArrayOf("varchar", roles));
    statement.setBytes(10, GeometryUtils.serialize(value.getGeometry()));
  }
}
