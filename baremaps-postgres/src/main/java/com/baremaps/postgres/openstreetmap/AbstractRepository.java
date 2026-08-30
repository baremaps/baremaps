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

import com.baremaps.postgres.copy.CopyWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.postgresql.PGConnection;
import org.postgresql.copy.PGCopyOutputStream;

/**
 * Stores entities keyed by an {@code int8} primary key in a Postgres table.
 *
 * <p>
 * Every repository of this package differs from the others only in the columns it occupies and in
 * how a row becomes an entity, so a subclass supplies those and inherits the statements and the
 * JDBC plumbing. The first column is the key; the rest follow it, in the same order, through the
 * select list, the insert parameters, the binary copy and the row mapper.
 *
 * @param <V> the type of the entities
 */
abstract class AbstractRepository<V> implements Repository<Long, V> {

  private final DataSource dataSource;
  private final int columnCount;
  private final String selectFrom;
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
   * Constructs an {@code AbstractRepository}.
   *
   * @param dataSource the data source
   * @param schema the schema holding the table
   * @param table the table name
   * @param columns the columns of the table, the primary key first
   */
  protected AbstractRepository(
      DataSource dataSource,
      String schema,
      String table,
      List<Column> columns) {
    var qualifiedTable = "%s.%s".formatted(schema, table);
    var key = columns.get(0);
    var names = columns.stream().map(Column::name).collect(Collectors.joining(", "));
    var selections = columns.stream().map(Column::selection).collect(Collectors.joining(", "));
    var parameters = columns.stream().map(Column::parameter).collect(Collectors.joining(", "));
    // An import replays the same entity whenever a later block or changeset touches it, so an
    // insert has to overwrite the row it finds rather than fail on it.
    var assignments = columns.stream().skip(1)
        .map(column -> "%1$s = excluded.%1$s".formatted(column.name()))
        .collect(Collectors.joining(", "));
    var declarations = IntStream.range(0, columns.size())
        .mapToObj(index -> "  %s %s%s".formatted(columns.get(index).name(),
            columns.get(index).sqlType(), index == 0 ? " PRIMARY KEY" : ""))
        .collect(Collectors.joining(",\n"));

    this.dataSource = dataSource;
    this.columnCount = columns.size();
    this.selectFrom = "SELECT %s FROM %s ".formatted(selections, qualifiedTable);
    this.createTable =
        "CREATE TABLE IF NOT EXISTS %s (\n%s\n)".formatted(qualifiedTable, declarations);
    this.dropTable = "DROP TABLE IF EXISTS %s CASCADE".formatted(qualifiedTable);
    this.truncateTable = "TRUNCATE TABLE %s".formatted(qualifiedTable);
    this.select =
        "SELECT %s FROM %s WHERE %s = ?".formatted(selections, qualifiedTable, key.name());
    this.selectIn =
        "SELECT %s FROM %s WHERE %s = ANY (?)".formatted(selections, qualifiedTable, key.name());
    this.insert = "INSERT INTO %s (%s) VALUES (%s) ON CONFLICT (%s) DO UPDATE SET %s"
        .formatted(qualifiedTable, names, parameters, key.name(), assignments);
    this.delete = "DELETE FROM %s WHERE %s = ?".formatted(qualifiedTable, key.name());
    this.deleteIn = "DELETE FROM %s WHERE %s = ANY (?)".formatted(qualifiedTable, key.name());
    this.copy = "COPY %s (%s) FROM STDIN BINARY".formatted(qualifiedTable, names);
  }

  /**
   * Builds an entity from the current row, reading the columns in the order they were declared.
   *
   * @param row the result set, positioned on the row to read
   */
  protected abstract V read(ResultSet row) throws SQLException, IOException;

  /**
   * Binds an entity to the parameters of an insert, in the order the columns were declared.
   *
   * @param statement the statement to bind
   * @param entity the entity to write
   */
  protected abstract void write(PreparedStatement statement, V entity)
      throws SQLException, IOException;

  /**
   * Writes an entity as one row of a binary copy, in the order the columns were declared.
   *
   * @param writer the copy writer, positioned at the start of a row
   * @param entity the entity to write
   */
  protected abstract void write(CopyWriter writer, V entity) throws IOException;

  /** Creates the table if it does not exist. */
  void createTable() throws RepositoryException {
    execute(createTable);
  }

  /** Drops the table and everything that depends on it. */
  void dropTable() throws RepositoryException {
    execute(dropTable);
  }

  /** Removes every row of the table. */
  void truncateTable() throws RepositoryException {
    execute(truncateTable);
  }

  private void execute(String sql) throws RepositoryException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.execute();
    } catch (SQLException e) {
      throw new RepositoryException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public V get(Long key) throws RepositoryException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(select)) {
      statement.setObject(1, key);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? read(result) : null;
      }
    } catch (SQLException | IOException e) {
      throw new RepositoryException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<V> get(List<Long> keys) throws RepositoryException {
    if (keys.isEmpty()) {
      return List.of();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectIn)) {
      statement.setArray(1, connection.createArrayOf("int8", keys.toArray()));
      try (ResultSet result = statement.executeQuery()) {
        // A single query answers the whole batch; the rows come back in an arbitrary order and a
        // missing key comes back not at all, so they are indexed before being restated in the order
        // asked for, with a null standing in for each absent key.
        Map<Long, V> values = new HashMap<>();
        while (result.next()) {
          values.put(result.getLong(1), read(result));
        }
        return keys.stream().map(values::get).toList();
      }
    } catch (SQLException | IOException e) {
      throw new RepositoryException(e);
    }
  }

  /**
   * Reads the entities matching a clause appended to the table's select list, so that a subclass
   * can add a query of its own without restating the columns or touching JDBC.
   *
   * @param clause the clause to append, such as an {@code ORDER BY}; it takes no parameters
   */
  protected List<V> select(String clause) throws RepositoryException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectFrom + clause);
        ResultSet result = statement.executeQuery()) {
      var values = new ArrayList<V>();
      while (result.next()) {
        values.add(read(result));
      }
      return values;
    } catch (SQLException | IOException e) {
      throw new RepositoryException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void put(V value) throws RepositoryException {
    put(List.of(value));
  }

  /** {@inheritDoc} */
  @Override
  public void put(List<V> values) throws RepositoryException {
    if (values.isEmpty()) {
      return;
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(insert)) {
      for (V value : values) {
        statement.clearParameters();
        write(statement, value);
        statement.addBatch();
      }
      statement.executeBatch();
    } catch (SQLException | IOException e) {
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
  public void copy(List<V> values) throws RepositoryException {
    if (values.isEmpty()) {
      return;
    }
    try (Connection connection = dataSource.getConnection()) {
      PGConnection pgConnection = connection.unwrap(PGConnection.class);
      try (CopyWriter writer = new CopyWriter(new PGCopyOutputStream(pgConnection, copy))) {
        writer.writeHeader();
        for (V value : values) {
          writer.startRow(columnCount);
          write(writer, value);
        }
      }
    } catch (IOException | SQLException e) {
      throw new RepositoryException(e);
    }
  }
}
