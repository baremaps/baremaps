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

package com.baremaps.config;

import static com.baremaps.testing.TestFiles.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.maplibre.map.MapCompiler;
import com.baremaps.maplibre.map.MapSpec;
import com.baremaps.maplibre.tilejson.TileJSON;
import com.baremaps.maplibre.tileset.Tileset;
import com.baremaps.utils.ObjectMapperUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class TileSetTest {

  final ObjectMapper objectMapper = ObjectMapperUtils.objectMapper();
  final ConfigReader configReader = new ConfigReader();

  /**
   * The basemap is described by a single specification, and its tileset is derived from it. Reading
   * the real one here keeps the derivation honest about a map of that size: the layer order, the
   * database it points at, and the fact that every source layer the recipe declares survives to the
   * tileset.
   */
  @Test
  public void testBasemapMapSpec() throws IOException {
    var path = Path.of("../basemap/map.js");

    var spec = objectMapper.readValue(configReader.read(path), MapSpec.class);
    var tileSet = MapCompiler.tileset(spec);
    var tileJSON = objectMapper.convertValue(tileSet, TileJSON.class);

    assertEquals("jdbc:postgresql://localhost:5432/baremaps?&user=baremaps&password=baremaps",
        tileSet.getDatabase());
    // The terrain is traced from elevation rather than queried, so it travels in the same tiles as
    // a layer the tileset describes and no query produces.
    var terrainLayers = spec.terrain() == null ? List.<String>of() : MapSpec.Terrain.LAYERS;
    var sourceLayers = spec.layers().stream()
        .filter(layer -> layer.getSourceLayer() != null)
        .filter(layer -> !terrainLayers.contains(layer.getSourceLayer()))
        .map(com.baremaps.maplibre.map.MapLayer::getSourceLayer)
        .distinct().toList();
    // The source layers follow the paint order, so the first one painted comes first.
    assertEquals(sourceLayers.get(0), tileJSON.getVectorLayers().get(0).id());
    assertEquals(sourceLayers.size() + terrainLayers.size(), tileSet.getVectorLayers().size());

    // Every layer reads the one source, the ones drawing the terrain included.
    assertEquals(Set.of("baremaps"), MapCompiler.style(spec).getSources().keySet());
    for (var id : terrainLayers) {
      var layer = tileSet.getVectorLayers().stream()
          .filter(vectorLayer -> vectorLayer.getId().equals(id))
          .findFirst().orElseThrow();
      assertTrue(layer.getQueries().isEmpty(), id);
    }

    // Nothing in the style draws with a feature identifier, so the queries do not select one.
    assertFalse(tileSet.isFeatureIds());
    for (var layer : tileSet.getVectorLayers()) {
      for (var query : layer.getQueries()) {
        assertFalse(query.getSql().startsWith("SELECT id,"), layer.getId());
      }
    }
  }

  @Test
  public void validateTileset() throws IOException {
    // Mapping to a POJO for baremaps-core and baremaps-server
    var tileSet = objectMapper.readValue(TILESET_JSON.toFile(), Tileset.class);
    // Mapping to a POJO strictly following TileJSON specifications for API clients.
    var tileJSON = objectMapper.readValue(TILESET_JSON.toFile(), TileJSON.class);

    assertEquals("jdbc:postgresql://localhost:5432/baremaps?&user=baremaps&password=baremaps",
        tileSet.getDatabase());
    assertEquals("aeroway", tileJSON.getVectorLayers().get(0).id());
  }

  @Test
  public void validateSpecificationExample() throws IOException {
    // Mapping to a POJO for baremaps-core and baremaps-server
    var tileSet = objectMapper.readValue(TILEJSON_JSON.toFile(), Tileset.class);
    // Mapping to a POJO strictly following TileJSON specifications for API clients.
    var tileJSON = objectMapper.readValue(TILEJSON_JSON.toFile(), TileJSON.class);

    assertNull(tileSet.getDatabase());
    assertEquals("layer_a", tileJSON.getVectorLayers().get(0).id());
  }
}
