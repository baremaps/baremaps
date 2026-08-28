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

package com.baremaps.tilestore.mbtiles;


import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileEntry;
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.TileStoreException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * A {@code TileStore} implementation that uses the
 * <a href="https://docs.mapbox.com/help/glossary/mbtiles/">MBTiles</a> file format for storing
 * tiles.
 */
public class MBTilesStore implements TileStore<ByteBuffer> {

  public static final String CREATE_TABLE_METADATA =
      "CREATE TABLE IF NOT EXISTS metadata (name TEXT, value TEXT, PRIMARY KEY (name))";

  private static final String CREATE_TABLE_TILES =
      "CREATE TABLE IF NOT EXISTS tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB, PRIMARY KEY (zoom_level, tile_column, tile_row))";

  private static final String CREATE_INDEX_TILES =
      "CREATE UNIQUE INDEX tile_index on tiles (zoom_level, tile_column, tile_row)";

  private static final String SELECT_METADATA = "SELECT name, value FROM metadata";

  private static final String SELECT_TILE =
      "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";

  private static final String INSERT_METADATA = "INSERT INTO metadata (name, value) VALUES (?, ?)";

  private static final String INSERT_TILE =
      "INSERT INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)";

  private static final String DELETE_TILE =
      "DELETE FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";

  private static final String DELETE_METADATA = "DELETE FROM metadata";

  private final DataSource dataSource;

  /**
   * Constructs an {@code MBTiles} with the provided SQLite datasource.
   *
   * @param dataSource the SQLite datasource
   */
  public MBTilesStore(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ByteBuffer read(TileCoord tileCoord) throws TileStoreException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_TILE)) {
      statement.setInt(1, tileCoord.z());
      statement.setInt(2, tileCoord.x());
      statement.setInt(3, reverseY(tileCoord.y(), tileCoord.z()));
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return ByteBuffer.wrap(resultSet.getBytes("tile_data"));
        } else {
          throw new SQLException("The tile does not exist");
        }
      }
    } catch (SQLException e) {
      throw new TileStoreException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(TileCoord tileCoord, ByteBuffer blob) throws TileStoreException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(INSERT_TILE)) {
      statement.setInt(1, tileCoord.z());
      statement.setInt(2, tileCoord.x());
      statement.setInt(3, reverseY(tileCoord.y(), tileCoord.z()));
      statement.setBytes(4, blob.array());
      statement.execute();
    } catch (SQLException e) {
      throw new TileStoreException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(List<TileEntry<ByteBuffer>> tileEntries) throws TileStoreException {
    try (
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(INSERT_TILE)) {
      for (TileEntry<ByteBuffer> tileEntry : tileEntries) {
        TileCoord tileCoord = tileEntry.getTileCoord();
        ByteBuffer byteBuffer = tileEntry.getTileValue();
        statement.setInt(1, tileCoord.z());
        statement.setInt(2, tileCoord.x());
        statement.setInt(3, reverseY(tileCoord.y(), tileCoord.z()));
        statement.setBytes(4, byteBuffer.array());
        statement.execute();
      }
    } catch (SQLException e) {
      throw new TileStoreException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(TileCoord tileCoord) throws TileStoreException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(DELETE_TILE)) {
      statement.setInt(1, tileCoord.z());
      statement.setInt(2, tileCoord.x());
      statement.setInt(3, reverseY(tileCoord.y(), tileCoord.z()));
      statement.execute();
    } catch (SQLException e) {
      throw new TileStoreException(e);
    }
  }

  /**
   * Initializes the SQLite database.
   *
   * @throws TileStoreException
   */
  public void initializeDatabase() throws TileStoreException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(CREATE_TABLE_METADATA);
      statement.execute(CREATE_TABLE_TILES);
      statement.execute(CREATE_INDEX_TILES);
    } catch (SQLException ex) {
      throw new TileStoreException(ex);
    }
  }

  /**
   * Reads the MBTiles metadata.
   *
   * @return the metadata
   * @throws IOException
   */
  public Map<String, String> readMetadata() throws IOException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_METADATA);
        ResultSet resultSet = statement.executeQuery()) {
      Map<String, String> metadata = new HashMap<>();
      while (resultSet.next()) {
        String name = resultSet.getString("name");
        String value = resultSet.getString("value");
        metadata.put(name, value);
      }
      return metadata;
    } catch (SQLException ex) {
      throw new IOException(ex);
    }
  }

  /**
   * Writes the MBTiles metadata.
   *
   * @param metadata the metadata
   * @throws IOException
   */
  public void writeMetadata(Map<String, String> metadata) throws IOException {
    try (Connection connection = dataSource.getConnection()) {
      try (Statement statement = connection.createStatement()) {
        statement.execute(DELETE_METADATA);
      }
      try (PreparedStatement statement = connection.prepareStatement(INSERT_METADATA)) {
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
          statement.setString(1, entry.getKey());
          statement.setString(2, entry.getValue());
          statement.executeUpdate();
        }
      }
    } catch (SQLException ex) {
      throw new IOException(ex);
    }
  }

  private static int reverseY(int y, int z) {
    return (int) (Math.pow(2.0, z) - 1 - y);
  }

  @Override
  public void close() throws Exception {
    // Do nothing
  }
}
