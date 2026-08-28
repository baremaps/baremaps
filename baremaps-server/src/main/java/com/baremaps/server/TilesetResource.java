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

import com.baremaps.maplibre.tileset.Tileset;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.ProducesJson;
import java.util.function.Supplier;

/**
 * A resource that provides access to the tileset file. Only suitable for development purposes, as
 * it exposes SQL queries.
 */
public class TilesetResource {

  private final Supplier<Tileset> tilesetSupplier;

  public TilesetResource(Supplier<Tileset> tilesetSupplier) {
    this.tilesetSupplier = tilesetSupplier;
  }

  @Get("/tiles.json")
  @ProducesJson
  public Tileset getTileset() {
    var tileset = tilesetSupplier.get();
    // Hide the database connection
    tileset.setDatabase(null);
    return tileset;
  }

}
