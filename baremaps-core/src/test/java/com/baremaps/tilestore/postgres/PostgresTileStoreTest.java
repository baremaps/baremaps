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

package com.baremaps.tilestore.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.baremaps.maplibre.tileset.Tileset;
import com.baremaps.maplibre.tileset.TilesetLayer;
import com.baremaps.maplibre.tileset.TilesetQuery;
import com.baremaps.tilestore.TileCoord;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostgresTileStoreTest {

  private Tileset tileset;

  @BeforeEach
  void prepare() {
    tileset = new Tileset();
    tileset.setMinzoom(0);
    tileset.setMaxzoom(20);
    tileset.setVectorLayers(List.of(
        new TilesetLayer("a", Map.of(), "", 0, 20,
            List.of(new TilesetQuery(0, 20, "SELECT id, tags, geom FROM table"))),
        new TilesetLayer("b", Map.of(), "", 0, 20,
            List.of(new TilesetQuery(0, 20, "SELECT id, tags, geom FROM table")))));

  }

  @Test
  void prepareNewQuery() {
    var postgresTileStore = new PostgresTileStore(null, tileset, 16);
    var query = postgresTileStore.prepareQuery(new TileCoord(1, 1, 10));
    assertEquals(
        "SELECT (SELECT ST_AsMVT(mvtGeom.*, 'a') FROM (SELECT mvtData.id AS id, mvtData.tags - 'id' AS tags, ST_AsMVTGeom(mvtData.geom, ST_TileEnvelope(?, ?, ?)) AS geom FROM (SELECT id, tags, geom FROM table) AS mvtData WHERE mvtData.geom IS NOT NULL AND mvtData.geom && ST_TileEnvelope(?, ?, ?, margin => (64.0/4096)) ) AS mvtGeom) || (SELECT ST_AsMVT(mvtGeom.*, 'b') FROM (SELECT mvtData.id AS id, mvtData.tags - 'id' AS tags, ST_AsMVTGeom(mvtData.geom, ST_TileEnvelope(?, ?, ?)) AS geom FROM (SELECT id, tags, geom FROM table) AS mvtData WHERE mvtData.geom IS NOT NULL AND mvtData.geom && ST_TileEnvelope(?, ?, ?, margin => (64.0/4096)) ) AS mvtGeom) AS mvtTile",
        query.sql());
  }

  @Test
  void prepareLegacyQuery() {
    var postgresTileStore = new PostgresTileStore(null, tileset, 15);
    var query = postgresTileStore.prepareQuery(new TileCoord(1, 1, 10));
    assertEquals(
        "SELECT (SELECT ST_AsMVT(mvtGeom.*, 'a') FROM (SELECT mvtData.id AS id, mvtData.tags - 'id' AS tags, ST_AsMVTGeom(mvtData.geom, ST_TileEnvelope(?, ?, ?)) AS geom FROM (SELECT id, tags, geom FROM table WHERE geom IS NOT NULL AND geom && ST_TileEnvelope(?, ?, ?, margin => (64.0/4096))) as mvtData ) AS mvtGeom) || (SELECT ST_AsMVT(mvtGeom.*, 'b') FROM (SELECT mvtData.id AS id, mvtData.tags - 'id' AS tags, ST_AsMVTGeom(mvtData.geom, ST_TileEnvelope(?, ?, ?)) AS geom FROM (SELECT id, tags, geom FROM table WHERE geom IS NOT NULL AND geom && ST_TileEnvelope(?, ?, ?, margin => (64.0/4096))) as mvtData ) AS mvtGeom) AS mvtTile",
        query.sql());
  }
}
