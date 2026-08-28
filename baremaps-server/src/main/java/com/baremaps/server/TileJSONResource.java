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

import com.baremaps.maplibre.tilejson.TileJSON;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.ProducesJson;
import java.util.function.Supplier;

/**
 * A resource that provides access to the tileJSON file.
 */
public class TileJSONResource {

  private final Supplier<TileJSON> tileJSONSupplier;

  public TileJSONResource(Supplier<TileJSON> tileJSONSupplier) {
    this.tileJSONSupplier = tileJSONSupplier;
  }

  @Get("/tiles.json")
  @ProducesJson
  public TileJSON getTileset() {
    return tileJSONSupplier.get();
  }

}
