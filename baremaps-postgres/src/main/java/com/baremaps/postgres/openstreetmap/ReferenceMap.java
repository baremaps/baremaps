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



import com.baremaps.data.collection.DataCollectionException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import javax.sql.DataSource;

/**
 * A read-only {@code LongDataMap} for references baked by OpenStreetMap ways stored in Postgres.
 */
public class ReferenceMap extends PostgresMap<Long, List<Long>> {

  private final DataSource dataSource;

  public final String selectContainsKey;

  public final String selectContainsValue;

  private final String selectIn;

  private final String selectById;

  private final String selectSize;

  private final String selectKeys;

  private final String selectValues;

  private final String selectEntries;

  /**
   * Constructs a {@code PostgresReferenceMap}.
   */
  public ReferenceMap(DataSource dataSource) {
    this(dataSource, "public", "osm_way");
  }

  /**
   * Constructs a {@code PostgresReferenceMap}.
   */
  public ReferenceMap(DataSource dataSource, String schema, String table) {
    this.dataSource = dataSource;
    var fullTableName = String.format("%s.%s", schema, table);
    this.selectContainsKey = String.format("""
        SELECT 1
        FROM %1$s
        WHERE id = ? LIMIT 1
        """, fullTableName);
    this.selectContainsValue = String.format("""
        SELECT 1
        FROM %1$s
        WHERE nodes = ? LIMIT 1
        """, fullTableName);
    this.selectIn = String.format("""
        SELECT id, nodes
        FROM %1$s
        WHERE id = ANY (?)
        """, fullTableName);
    this.selectById = String.format("""
        SELECT nodes
        FROM %1$s
        WHERE id = ?
        """, fullTableName);
    this.selectSize = String.format("""
        SELECT count()
        FROM %1$s
        """, fullTableName);
    this.selectKeys = String.format("""
        SELECT id
        FROM %1$s
        """, fullTableName);
    this.selectValues = String.format("""
        SELECT nodes
        FROM %1$s
        """, fullTableName);
    this.selectEntries = String.format("""
        SELECT id, nodes
        FROM %1$s
        """, fullTableName);
  }

  @Override
  public long sizeAsLong() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectSize)) {
      try (ResultSet result = statement.executeQuery()) {
        if (result.next()) {
          return result.getLong(1);
        } else {
          throw new DataCollectionException();
        }
      }
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  @Override
  public boolean containsKey(Object key) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectContainsKey)) {
      statement.setLong(1, (Long) key);
      try (ResultSet result = statement.executeQuery()) {
        return result.next();
      }
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  @Override
  public boolean containsValue(Object value) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectContainsValue)) {
      statement.setArray(1, connection.createArrayOf("int8", (Long[]) value));
      try (ResultSet result = statement.executeQuery()) {
        return result.next();
      }
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Long> get(Object key) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectById)) {
      statement.setLong(1, (Long) key);
      try (ResultSet result = statement.executeQuery()) {
        if (result.next()) {
          Array array = result.getArray(1);
          return array == null ? Collections.emptyList() : Arrays.asList((Long[]) array.getArray());
        } else {
          throw new IllegalArgumentException();
        }
      }
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<List<Long>> getAll(List<Long> keys) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectIn)) {
      statement.setArray(1, connection.createArrayOf("int8", keys.toArray()));
      try (ResultSet result = statement.executeQuery()) {
        Map<Long, List<Long>> referenceMap = new HashMap<>();
        while (result.next()) {
          long key = result.getLong(1);
          Array array = result.getArray(2);
          referenceMap.put(key,
              array == null ? List.of() : Arrays.asList((Long[]) array.getArray()));
        }
        return keys.stream().map(referenceMap::get).toList();
      }
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  @Override
  protected Iterator<Long> keyIterator() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectKeys)) {
      ResultSet result = statement.executeQuery();
      return new ResultSetIterator<>(result, this::key);
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  private Long key(ResultSet resultSet) {
    try {
      return resultSet.getLong(1);
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  @Override
  protected Iterator<List<Long>> valueIterator() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectValues)) {
      ResultSet result = statement.executeQuery();
      return new ResultSetIterator<>(result, this::value);
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  private List<Long> value(ResultSet resultSet) {
    try {
      Array array = resultSet.getArray(1);
      return array == null ? List.of() : Arrays.asList((Long[]) array.getArray());
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  @Override
  protected Iterator<Entry<Long, List<Long>>> entryIterator() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(selectEntries)) {
      ResultSet result = statement.executeQuery();
      return new ResultSetIterator<>(result, this::entry);
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  private Entry<Long, List<Long>> entry(ResultSet resultSet) {
    try {
      long key = resultSet.getLong(1);
      Array array = resultSet.getArray(2);
      List<Long> value = array == null ? List.of() : Arrays.asList((Long[]) array.getArray());
      return Map.entry(key, value);
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  @Override
  public List<Long> put(Long key, List<Long> value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Long> remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }
}
