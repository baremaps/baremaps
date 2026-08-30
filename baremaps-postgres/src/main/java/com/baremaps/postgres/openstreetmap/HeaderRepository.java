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

import com.baremaps.openstreetmap.model.Header;
import com.baremaps.postgres.copy.CopyWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;

/**
 * Stores the replication headers of an OpenStreetMap database.
 *
 * <p>
 * Each header records the replication sequence an import or an update reached, so the newest one
 * tells the next update where to resume from.
 */
public class HeaderRepository extends AbstractRepository<Header> {

  private static final String NEWEST_FIRST = "ORDER BY replication_sequence_number DESC";

  private static final List<Column> COLUMNS = List.of(
      Column.of("replication_sequence_number", "int8"),
      Column.of("replication_timestamp", "timestamp without time zone"),
      Column.of("replication_url", "text"),
      Column.of("source", "text"),
      Column.of("writing_program", "text"));

  /** Constructs a {@code HeaderRepository} over the default header table. */
  public HeaderRepository(DataSource dataSource) {
    this(dataSource, "public", "osm_header");
  }

  /** Constructs a {@code HeaderRepository} over the given header table. */
  public HeaderRepository(DataSource dataSource, String schema, String table) {
    super(dataSource, schema, table, COLUMNS);
  }

  /** Returns every header, the highest replication sequence number first. */
  public List<Header> selectAll() throws RepositoryException {
    return select(NEWEST_FIRST);
  }

  /**
   * Returns the header with the highest replication sequence number, or {@code null} if the
   * database has never been imported.
   */
  public Header selectLatest() throws RepositoryException {
    var headers = select(NEWEST_FIRST + " LIMIT 1");
    return headers.isEmpty() ? null : headers.get(0);
  }

  /** {@inheritDoc} */
  @Override
  protected Header read(ResultSet row) throws SQLException {
    return new Header(
        row.getLong(1),
        row.getObject(2, LocalDateTime.class),
        row.getString(3),
        row.getString(4),
        row.getString(5));
  }

  /** {@inheritDoc} */
  @Override
  protected void write(PreparedStatement statement, Header header) throws SQLException {
    statement.setObject(1, header.replicationSequenceNumber());
    statement.setObject(2, header.replicationTimestamp());
    statement.setObject(3, header.replicationUrl());
    statement.setObject(4, header.source());
    statement.setObject(5, header.writingProgram());
  }

  /** {@inheritDoc} */
  @Override
  protected void write(CopyWriter writer, Header header) throws IOException {
    writer.writeLong(header.replicationSequenceNumber());
    writer.writeLocalDateTime(header.replicationTimestamp());
    writer.write(header.replicationUrl());
    writer.write(header.source());
    writer.write(header.writingProgram());
  }
}
