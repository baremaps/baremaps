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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Streams the rows of a query, mapping each one as it is read.
 *
 * <p>
 * The iterator owns the connection, statement and result set, and releases them as soon as the last
 * row has been read. An iterator that is abandoned before exhaustion must therefore be closed, or
 * its connection is held until the pool reclaims it; the collection views of {@link PostgresMap}
 * are the only callers, and they are documented as such.
 */
class ResultSetIterator<T> implements Iterator<T>, AutoCloseable {

  /** Reads the current row of a result set, without advancing it. */
  @FunctionalInterface
  interface RowMapper<T> {
    T apply(ResultSet resultSet) throws SQLException;
  }

  private final Connection connection;
  private final Statement statement;
  private final ResultSet resultSet;
  private final RowMapper<T> mapper;

  /**
   * The row read ahead of the caller. {@link Iterator#hasNext()} has to know whether a row follows,
   * and the only way to ask a {@code ResultSet} that is to advance it, so the row it lands on is
   * held here until {@link #next()} claims it. Without this buffer a caller that consults
   * {@code hasNext()} before every element would see every other row.
   */
  private T lookahead;

  private boolean exhausted;

  ResultSetIterator(Connection connection, Statement statement, ResultSet resultSet,
      RowMapper<T> mapper) {
    this.connection = connection;
    this.statement = statement;
    this.resultSet = resultSet;
    this.mapper = mapper;
  }

  @Override
  public boolean hasNext() {
    if (lookahead != null) {
      return true;
    }
    if (exhausted) {
      return false;
    }
    try {
      if (resultSet.next()) {
        lookahead = mapper.apply(resultSet);
        return true;
      }
      exhausted = true;
      close();
      return false;
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }

  @Override
  public T next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    T value = lookahead;
    lookahead = null;
    return value;
  }

  /** Releases the result set, the statement and the connection, in that order. */
  @Override
  public void close() {
    try (connection; statement; resultSet) {
      // The resources are closed by the try-with-resources block.
    } catch (SQLException e) {
      throw new DataCollectionException(e);
    }
  }
}
