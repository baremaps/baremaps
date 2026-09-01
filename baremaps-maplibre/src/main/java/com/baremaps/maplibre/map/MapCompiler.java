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

package com.baremaps.maplibre.map;


import com.baremaps.maplibre.style.Style;
import com.baremaps.maplibre.style.StyleLayer;
import com.baremaps.maplibre.style.StyleSource;
import com.baremaps.maplibre.tileset.Tileset;
import com.baremaps.maplibre.tileset.TilesetLayer;
import com.baremaps.maplibre.tileset.TilesetQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Derives the style and the tileset from a {@link MapSpec}.
 *
 * <p>
 * Both come from the same list of layers. The style is that list with the extensions dropped and
 * the properties every layer shares filled in; the tileset is the queries those layers declare,
 * with each one selecting the attributes the style reads at that zoom and no others. That last part
 * is the difference between a tile carrying what is drawn and a tile carrying whatever the database
 * held.
 *
 * <p>
 * A query is declared over a range of zoom levels, but what the style reads changes within that
 * range: an icon layer that starts at zoom 14 makes its attributes worth carrying from 14 and not
 * before. Each declared range is therefore walked one zoom at a time and split where the demand
 * changes, so the emitted queries follow the style rather than the declaration.
 */
public final class MapCompiler {

  /** The zoom a query is taken to end at when it names no end, one past the deepest tile. */
  private static final int DEFAULT_MAXZOOM = 20;

  /** Metres per pixel at zoom zero, for a tile 512 pixels across. */
  private static final String RESOLUTION = "78270";

  private MapCompiler() {}

  /**
   * Returns the style described by the specification.
   *
   * @param spec the specification
   * @return the style
   */
  public static Style style(MapSpec spec) {
    var source = spec.source();

    var vector = new StyleSource();
    vector.setType("vector");
    vector.setUrl(source.url());

    var sources = new LinkedHashMap<String, StyleSource>();
    sources.put(source.id(), vector);

    // The terrain is a second source because it is not a second query: it is traced from an
    // elevation archive rather than selected from the database, so it has a tileset of nothing and
    // reaches the style directly.
    if (spec.terrain() != null) {
      var terrain = spec.terrain();
      sources.put(terrain.id(), new StyleSource()
          .setType("vector")
          .setTiles(terrain.tiles())
          .setBounds(terrain.bounds())
          .setMinzoom(terrain.minzoom())
          .setMaxzoom(terrain.maxzoom())
          .setAttribution(terrain.attribution()));
    }

    var layers = new ArrayList<StyleLayer>();
    for (var layer : spec.layers()) {
      layers.add(new StyleLayer()
          .setId(layer.getId())
          .setType(layer.getType())
          .setFilter(layer.getFilter())
          // Every layer reads the one source, so no layer says so.
          .setSource(layer.getSource() == null ? source.id() : layer.getSource())
          .setSourceLayer(layer.getSourceLayer())
          .setLayout(layer.getLayout())
          .setMinzoom(layer.getMinzoom())
          .setMaxzoom(layer.getMaxzoom())
          .setPaint(layer.getPaint()));
    }

    return new Style()
        .setVersion(8)
        .setName(spec.name())
        .setCenter(spec.center())
        .setZoom(spec.zoom())
        .setSprite(spec.sprite())
        .setGlyphs(spec.glyphs())
        .setSources(sources)
        .setLayers(layers);
  }

  /**
   * Returns the tileset described by the specification, with the attributes of every query derived
   * from the style.
   *
   * @param spec the specification
   * @return the tileset
   */
  public static Tileset tileset(MapSpec spec) {
    var style = style(spec);
    var source = spec.source();
    int minzoom = source.minzoom();
    int maxzoom = source.maxzoom();
    var featureIds = source.featureIds();

    // The demand of every source layer, at every zoom the tileset covers. Computed once: reading it
    // walks the whole style, and every query below consults it.
    var demand = new HashMap<Integer, Map<String, Demand.Attributes>>();
    for (int zoom = minzoom; zoom <= maxzoom; zoom++) {
      demand.put(zoom, Demand.of(style, zoom));
    }

    var generalized = new HashMap<String, MapLayer>();
    for (var layer : spec.layers()) {
      if (layer.getGeneralize() != null) {
        generalized.put(layer.getSourceLayer(), layer);
      }
    }

    var layers = new ArrayList<TilesetLayer>();
    for (var entry : queries(spec).entrySet()) {
      var id = entry.getKey();
      var compiled = new ArrayList<TilesetQuery>();
      var fields = new LinkedHashMap<String, String>();
      for (var query : split(entry.getValue(), generalized.get(id))) {
        compiled.addAll(compile(id, query, demand, minzoom, maxzoom, fields, featureIds));
      }
      if (!compiled.isEmpty()) {
        layers.add(new TilesetLayer()
            .setId(id)
            .setFields(fields)
            .setMinzoom(compiled.get(0).getMinzoom())
            .setMaxzoom(compiled.get(compiled.size() - 1).getMaxzoom())
            .setQueries(compiled));
      }
    }

    var tileset = new Tileset()
        .setName(spec.name())
        .setAttribution(source.attribution())
        .setBounds(source.bounds())
        .setCenter(center(spec))
        .setMinzoom(minzoom)
        .setMaxzoom(maxzoom)
        .setTiles(source.tiles())
        .setFeatureIds(featureIds)
        .setVectorLayers(layers);
    tileset.setDatabase(spec.database());
    return tileset;
  }

  /**
   * Splits a generalized layer's queries at the zoom the generalization starts, so that the levels
   * below read the views the engine builds and the levels above read the relation itself. A layer
   * that is not generalized is left alone.
   */
  private static List<MapSpec.Query> split(List<MapSpec.Query> queries, MapLayer generalized) {
    if (generalized == null) {
      return queries;
    }
    var relation = SchemaCompiler.relation(generalized);
    var below = SchemaCompiler.below(generalized);
    var split = new ArrayList<MapSpec.Query>();
    for (var query : queries) {
      var from = query.minzoom() == null ? 0 : query.minzoom();
      var to = query.maxzoom() == null ? DEFAULT_MAXZOOM + 1 : query.maxzoom();
      if (from < below) {
        // The tile store substitutes the zoom, so one query covers every generalized level.
        split.add(new MapSpec.Query(from, Math.min(to, below),
            SchemaCompiler.view(relation, 0).replace("_z0", "_z$zoom"),
            query.filter(), query.where(), query.simplify(), query.drawable()));
      }
      if (to > below) {
        split.add(new MapSpec.Query(Math.max(from, below), to, relation, query.filter(),
            query.where(), query.simplify(), query.drawable()));
      }
    }
    return split;
  }

  /**
   * The queries of every source layer, keyed in the order their source layers are first painted.
   *
   * <p>
   * A source layer feeds several style layers, so exactly one of them declares where the features
   * come from. Declaring it twice would be two answers to one question, and not declaring it at all
   * would leave a layer with nothing to draw, so both are refused rather than resolved.
   */
  static Map<String, List<MapSpec.Query>> queries(MapSpec spec) {
    var queries = new LinkedHashMap<String, List<MapSpec.Query>>();
    var declaredBy = new HashMap<String, String>();

    var terrain = spec.terrain() == null ? null : spec.terrain().id();
    for (var layer : spec.layers()) {
      var id = layer.getSourceLayer();
      if (id == null) {
        continue;
      }
      // A layer reading the terrain reads tiles that are computed rather than queried, so it
      // neither declares a query nor leaves one owing.
      if (terrain != null && terrain.equals(layer.getSource())) {
        if (layer.getSourceQueries() != null) {
          throw new IllegalArgumentException(String.format(
              "The layer '%s' reads the terrain, which is traced from elevation, "
                  + "so it cannot declare source-queries.",
              layer.getId()));
        }
        continue;
      }
      queries.putIfAbsent(id, null);
      if (layer.getSourceQueries() == null) {
        continue;
      }
      if (declaredBy.containsKey(id)) {
        throw new IllegalArgumentException(String.format(
            "The source layer '%s' is declared twice, by '%s' and by '%s'. "
                + "Exactly one layer names where a source layer comes from.",
            id, declaredBy.get(id), layer.getId()));
      }
      declaredBy.put(id, layer.getId());
      queries.put(id, layer.getSourceQueries());
    }

    for (var entry : queries.entrySet()) {
      if (entry.getValue() == null) {
        throw new IllegalArgumentException(String.format(
            "No layer says where the source layer '%s' comes from. "
                + "Add source-queries to one of the layers that reads it.",
            entry.getKey()));
      }
    }
    return queries;
  }

  /**
   * Splits one declared query at the zoom levels where what the style reads changes, and drops the
   * levels where it reads nothing at all.
   */
  private static List<TilesetQuery> compile(String id, MapSpec.Query query,
      Map<Integer, Map<String, Demand.Attributes>> demand, int minzoom, int maxzoom,
      Map<String, String> fields, boolean featureIds) {
    int from = Math.max(query.minzoom() == null ? minzoom : query.minzoom(), minzoom);
    int to = Math.min(query.maxzoom() == null ? maxzoom + 1 : query.maxzoom(), maxzoom + 1);

    var queries = new ArrayList<TilesetQuery>();
    SortedSet<String> current = null;
    int start = from;
    for (int zoom = from; zoom <= to; zoom++) {
      // One past the end closes the last run.
      var attributes = zoom < to ? demand.get(zoom).get(id) : null;
      var keys = attributes == null ? null : attributes.keys();
      if (current != null && current.equals(keys)) {
        continue;
      }
      if (current != null) {
        queries.add(new TilesetQuery(start, zoom, sql(query, current, featureIds)));
      }
      current = keys;
      start = zoom;
      if (keys != null) {
        keys.forEach(key -> fields.put(key, "String"));
      }
    }
    return queries;
  }

  /**
   * A feature carrying none of the attributes the style reads at this zoom cannot match any of its
   * filters, so it can only be drawn as nothing. Asking for it costs a row in the query, a feature
   * in the tile and bytes on the wire, and the set of attributes to test is the same one already
   * derived for the projection.
   */
  private static String drawable(SortedSet<String> keys) {
    if (keys.isEmpty()) {
      return null;
    }
    return "(" + keys.stream().map(key -> "tags ? " + literal(key))
        .collect(java.util.stream.Collectors.joining(" OR ")) + ")";
  }

  private static String sql(MapSpec.Query query, SortedSet<String> keys, boolean featureIds) {
    var sql = new StringBuilder("SELECT ")
        .append(featureIds ? "id, " : "")
        .append(tags(keys))
        .append(" AS tags, ")
        .append(geom(query))
        .append(" AS geom FROM ")
        .append(query.from());
    var condition = condition(query, keys);
    if (condition != null) {
      sql.append(" WHERE ").append(condition);
    }
    return sql.toString();
  }

  /**
   * The condition a query selects on: its filter, written as sql, and its raw predicate if it has
   * one. A query may carry both, for a filter that says most of it and a predicate that says the
   * rest.
   */
  private static String condition(MapSpec.Query query, SortedSet<String> keys) {
    var parts = new ArrayList<String>();
    var filter = FilterCompiler.sql(query.filter(), "tags");
    if (filter != null) {
      parts.add(filter);
    }
    if (query.where() != null && !query.where().isBlank()) {
      parts.add(query.where());
    }
    if (Boolean.TRUE.equals(query.drawable())) {
      var drawable = drawable(keys);
      if (drawable != null) {
        parts.add(drawable);
      }
    }
    return parts.isEmpty() ? null : String.join(" AND ", parts);
  }

  /** Selects the attributes the style reads, and nothing else. */
  private static String tags(SortedSet<String> keys) {
    if (keys.isEmpty()) {
      return "'{}'::jsonb";
    }
    var tags = new StringBuilder("jsonb_build_object(");
    var first = true;
    for (var key : keys) {
      if (!first) {
        tags.append(", ");
      }
      first = false;
      tags.append(literal(key)).append(", tags -> ").append(literal(key));
    }
    return tags.append(")").toString();
  }

  /**
   * The tolerance is written in terms of the zoom the tile is produced at, which the tile store
   * substitutes, so one query covers its whole range.
   */
  private static String geom(MapSpec.Query query) {
    if (query.simplify() == null) {
      return "geom";
    }
    return "ST_SimplifyPreserveTopology(geom, " + RESOLUTION + " / POWER(2, $zoom) * "
        + query.simplify() + ")";
  }

  private static String literal(String value) {
    return "'" + value.replace("'", "''") + "'";
  }

  /** A tileset centre carries the zoom as a third element, where a style centre holds two. */
  private static List<Double> center(MapSpec spec) {
    if (spec.center() == null) {
      return List.of();
    }
    var center = new ArrayList<Double>();
    spec.center().forEach(value -> center.add(value.doubleValue()));
    if (spec.zoom() != null) {
      center.add(spec.zoom().doubleValue());
    }
    return center;
  }

  /** Returns the tileset layers of a specification keyed by id, for reporting and tests. */
  static Map<String, TilesetLayer> layers(Tileset tileset) {
    var layers = new TreeMap<String, TilesetLayer>();
    tileset.getVectorLayers().forEach(layer -> layers.put(layer.getId(), layer));
    return layers;
  }
}
