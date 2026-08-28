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

package com.baremaps.tilestore.file;



import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.TileStoreException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Represents a {@code TileStore} baked by a directory. */
public class FileTileStore implements TileStore<ByteBuffer> {

  private final Path path;

  /**
   * Constructs a {@code FileTileStore}.
   *
   * @param path the directory
   */
  public FileTileStore(Path path) {
    this.path = path;
  }

  /** {@inheritDoc} */
  @Override
  public ByteBuffer read(TileCoord tileCoord) throws TileStoreException {
    try {
      return ByteBuffer.wrap(Files.readAllBytes(resolve(tileCoord)));
    } catch (IOException e) {
      throw new TileStoreException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void write(TileCoord tileCoord, ByteBuffer blob) throws TileStoreException {
    try {
      var file = resolve(tileCoord);
      Files.createDirectories(file.getParent());
      Files.write(file, blob.array());
    } catch (IOException e) {
      throw new TileStoreException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(TileCoord tileCoord) throws TileStoreException {
    try {
      Files.deleteIfExists(resolve(tileCoord));
    } catch (IOException e) {
      throw new TileStoreException(e);
    }
  }

  /** {@inheritDoc} */
  public Path resolve(TileCoord tileCoord) {
    return path.resolve(String.format("%s/%s/%s.mvt", tileCoord.z(), tileCoord.x(), tileCoord.y()));
  }

  @Override
  public void close() {
    // Do nothing
  }
}
