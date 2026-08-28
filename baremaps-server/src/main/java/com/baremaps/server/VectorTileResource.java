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

package com.baremaps.server;

import static com.google.common.net.HttpHeaders.*;

import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.TileStoreException;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.server.annotation.Blocking;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.Param;
import java.nio.ByteBuffer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resource that serves vector tiles as Mapbox Vector Tiles.
 */
public class VectorTileResource {

  private static final Logger logger = LoggerFactory.getLogger(VectorTileResource.class);

  public static final String TILE_ENCODING = "gzip";

  public static final String TILE_TYPE = "application/vnd.mapbox-vector-tile";

  private final Supplier<TileStore<ByteBuffer>> tileStoreSupplier;

  /**
   * Constructs a {@code VectorTileResource} with the specified tile store supplier.
   *
   * @param tileStoreSupplier the tile store supplier
   */
  public VectorTileResource(Supplier<TileStore<ByteBuffer>> tileStoreSupplier) {
    this.tileStoreSupplier = tileStoreSupplier;
  }

  /**
   * Serves a vector tile as a Mapbox Vector Tile.
   */
  @Get("regex:^/(?<z>[0-9]+)/(?<x>[0-9]+)/(?<y>[0-9]+).mvt$")
  @Blocking
  public HttpResponse tile(@Param("z") int z, @Param("x") int x, @Param("y") int y) {
    TileCoord tileCoord = new TileCoord(x, y, z);
    try {
      TileStore<ByteBuffer> tileStore = tileStoreSupplier.get();
      ByteBuffer blob = tileStore.read(tileCoord);
      if (blob != null) {
        var headers = ResponseHeaders.builder(200)
            .add(CONTENT_TYPE, TILE_TYPE)
            .add(CONTENT_ENCODING, TILE_ENCODING)
            .add(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
            .build();
        byte[] bytes = new byte[blob.remaining()];
        blob.get(bytes);
        HttpData data = HttpData.wrap(bytes);
        return HttpResponse.of(headers, data);

      } else {
        return HttpResponse.of(204);
      }
    } catch (TileStoreException ex) {
      logger.error("Error while reading tile {}", tileCoord, ex);
      return HttpResponse.of(404);
    }
  }

}
