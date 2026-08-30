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

import com.baremaps.data.collection.BatchMap;
import com.baremaps.data.collection.DataCollectionException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;

/**
 * A read-only view of a Postgres table as a map from its {@code int8} primary key to a value built
 * from the other selected columns.
 *
 * <p>
 * The OpenStreetMap import keeps an in-memory index of node coordinates and way references; this
 * class is the fallback used when a later update runs without that index, so the only operations it
 * has to answer well are {@link #get} and {@link #getAll}. The mutators of {@link Map} are
 * unsupported rather than emulated, since writing goes through the repositories.
 *
 * <p>
 * Every query selects the key first and the value columns after it, so a subclass supplies a single
 * mapper, {@link #readValue}, and the four queries below are all the SQL this hierarchy needs.
 *
 * @param <V> the type of the values
 */
abstract class PostgresMap<V> implements BatchMap<Long, V> {

  private final DataSource dataSource;
  private final String selectByKey;
  private final String selectByKeys;
  private final String selectAll;
  private final String selectCount;
  private final String selectAny;

  /**
   * Constructs a {@code PostgresMap}.
   *
   * @param dataSource the data source
   * @param schema the schema holding the table
   * @param table the table to read
   * @param keyColumn the {@code int8} primary key column
   * @param valueColumns the columns {@link #readValue} consumes, in the order it reads them
   */
  protected PostgresMap(
      DataSource dataSource,
      String schema,
      String table,
      String keyColumn,
      String... valueColumns) {
    var qualifiedTable = "%s.%s".formatted(schema, table);
    var columns = Stream.concat(Stream.of(keyColumn), Stream.of(valueColumns))
        .collect(Collectors.joining(", "));
    this.dataSource = dataSource;
    this.selectByKey =
        "SELECT %s FROM %s WHERE %s = ?".formatted(columns, qualifiedTable, keyColumn);
    this.selectByKeys =
        "SELECT %s FROM %s WHERE %s = ANY (?)".formatted(columns, qualifiedTable, keyColumn);
    this.selectAll = "SELECT %s FROM %s".formatted(columns, qualifiedTable);
    this.selectCount = "SELECT count(*) FROM %s".formatted(qualifiedTable);
    this.selectAny = "SELECT 1 FROM %s LIMIT 1".formatted(qualifiedTable);
  }

  /**
   * Builds a value from the current row, reading the value columns that follow the key. They start
   * at index 2.
   *
   * @param row the result set, positioned on the row to read
   * @return the value held by the row
   */
  protected abstract V readValue(ResultSet row) throws SQLException;

  /** {@inheritDoc} */
  @Override
  public V get(Object key) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectByKey)) {
      statement.setLong(1, (Long) key);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? readValue(result) : null;
      }
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<V> getAll(List<Long> keys) {
    if (keys.isEmpty()) {
      return List.of();
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectByKeys)) {
      statement.setArray(1, connection.createArrayOf("int8", keys.toArray()));
      try (ResultSet result = statement.executeQuery()) {
        // A single query answers the whole batch; the rows come back in an arbitrary order and in
        // an arbitrary number, so they are indexed before being restated in the order asked for.
        Map<Long, V> values = new HashMap<>();
        while (result.next()) {
          values.put(result.getLong(1), readValue(result));
        }
        return keys.stream().map(values::get).toList();
      }
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsKey(Object key) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectByKey)) {
      statement.setLong(1, (Long) key);
      try (ResultSet result = statement.executeQuery()) {
        return result.next();
      }
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * Unsupported: the values are built by {@link #readValue} from several columns, so a value cannot
   * be turned back into a predicate the database could answer, and scanning the table to compare
   * them row by row would be a trap at the size of these tables.
   */
  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEmpty() {
    // Cheaper than size(): the table holds hundreds of millions of rows and counting them all only
    // to compare against zero would read every one of them.
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectAny);
        ResultSet result = statement.executeQuery()) {
      return !result.next();
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  /**
   * Returns the number of rows in the table, saturating at {@link Integer#MAX_VALUE}.
   *
   * <p>
   * {@link Map#size()} is an {@code int} and these tables routinely exceed it, so prefer
   * {@link #sizeAsLong()} where the exact count matters.
   */
  @Override
  public int size() {
    return (int) Math.min(sizeAsLong(), Integer.MAX_VALUE);
  }

  /** Returns the number of rows in the table. */
  public long sizeAsLong() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectCount);
        ResultSet result = statement.executeQuery()) {
      if (!result.next()) {
        throw new DataCollectionException("count(*) returned no row");
      }
      return result.getLong(1);
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  /**
   * Streams every row of the table, mapping each one with {@code mapper}.
   *
   * <p>
   * The iterator holds a connection until it is exhausted or closed, which is why the views built
   * on it are documented as streaming.
   */
  private <T> ResultSetIterator<T> iterator(ResultSetIterator.RowMapper<T> mapper) {
    Connection connection = null;
    PreparedStatement statement = null;
    try {
      connection = dataSource.getConnection();
      statement = connection.prepareStatement(selectAll);
      return new ResultSetIterator<>(connection, statement, statement.executeQuery(), mapper);
    } catch (SQLException e) {
      closeQuietly(statement);
      closeQuietly(connection);
      throw new DataCollectionException(e);
    }
  }

  private static void closeQuietly(AutoCloseable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception suppressed) {
        // The caller is already throwing the failure that brought us here.
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * The view streams the table rather than materialising it. Its iterator holds a connection until
   * it is exhausted, so an iteration that stops early should close it.
   */
  @Override
  public Set<Long> keySet() {
    return new AbstractSet<>() {
      @Override
      public Iterator<Long> iterator() {
        return PostgresMap.this.iterator(row -> row.getLong(1));
      }

      @Override
      public int size() {
        return PostgresMap.this.size();
      }
    };
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * The view streams the table rather than materialising it. Its iterator holds a connection until
   * it is exhausted, so an iteration that stops early should close it.
   */
  @Override
  public Collection<V> values() {
    return new AbstractCollection<>() {
      @Override
      public Iterator<V> iterator() {
        return PostgresMap.this.iterator(PostgresMap.this::readValue);
      }

      @Override
      public int size() {
        return PostgresMap.this.size();
      }
    };
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * The view streams the table rather than materialising it. Its iterator holds a connection until
   * it is exhausted, so an iteration that stops early should close it.
   */
  @Override
  public Set<Entry<Long, V>> entrySet() {
    return new AbstractSet<>() {
      @Override
      public Iterator<Entry<Long, V>> iterator() {
        return PostgresMap.this
            .iterator(row -> Map.entry(row.getLong(1), PostgresMap.this.readValue(row)));
      }

      @Override
      public int size() {
        return PostgresMap.this.size();
      }
    };
  }

  /** {@inheritDoc} Unsupported: writing goes through the repositories. */
  @Override
  public V put(Long key, V value) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} Unsupported: writing goes through the repositories. */
  @Override
  public void putAll(Map<? extends Long, ? extends V> map) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} Unsupported: writing goes through the repositories. */
  @Override
  public V remove(Object key) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} Unsupported: writing goes through the repositories. */
  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }
}
