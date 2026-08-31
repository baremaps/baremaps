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


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes the materialized views a generalized source layer is read from.
 *
 * <p>
 * A layer that declares {@code generalize} says which of its values are worth showing once the map
 * zooms away, and how much detail they may lose. Everything else about the chain follows from that:
 * that a view exists per zoom level, what it is called, that each one reads the level above it, and
 * therefore the order they must be built and refreshed in. Those are bookkeeping, and were the part
 * that had to be written out by hand for every layer and every zoom level.
 *
 * <p>
 * The chain has two halves. Down to {@code mergeAbove}, neighbours sharing a value are dilated
 * until they touch, merged, eroded back and simplified; the identity of individual features is gone
 * by then, so the rows are renumbered and carry only the value they were merged on. Below it a
 * merge would span a continent, so the geometry is only simplified and the rows keep their
 * identity.
 */
public final class SchemaCompiler {

  /** Metres per pixel at zoom zero, for a tile 512 pixels across. */
  private static final String RESOLUTION = "78270";

  private static final int DEFAULT_BELOW = 13;
  private static final int DEFAULT_MERGE_ABOVE = 4;
  private static final double DEFAULT_AREA = 32;
  private static final double DEFAULT_AREA_BELOW_MERGE = 16;
  private static final double DEFAULT_BUFFER = 1.1;
  private static final double DEFAULT_LENGTH = 2;
  private static final String LINES = "lines";

  private SchemaCompiler() {}

  /**
   * Returns the statements that build every generalized source layer of a specification, in the
   * order they must be run.
   *
   * @param spec the specification
   * @return the statements, separated by semicolons
   */
  public static String sql(MapSpec spec) {
    var statements = new ArrayList<String>();
    for (var layer : spec.layers()) {
      if (layer.getGeneralize() != null) {
        statements.addAll(statements(relation(layer), layer.getGeneralize(), bottom(layer)));
      }
    }
    return statements.stream().map(statement -> statement + ";\n")
        .collect(Collectors.joining("\n"));
  }

  /**
   * The relation a generalized layer's chain is built from, and which its views are named after. A
   * layer declaring generalization names the relation once; the levels below it are this engine's
   * to name.
   */
  public static String relation(MapLayer layer) {
    var queries = layer.getSourceQueries();
    if (queries == null || queries.isEmpty() || queries.get(0).from() == null) {
      throw new IllegalArgumentException(String.format(
          "Layer '%s' declares generalize but no query to generalize.", layer.getId()));
    }
    return queries.get(0).from();
  }

  /** The view a generalized layer is read from at one zoom level. */
  public static String view(String relation, int zoom) {
    return "%s_z%d".formatted(relation, zoom);
  }

  /** The first zoom level that is generalized, counting down. */
  public static int below(MapLayer layer) {
    return below(layer.getGeneralize());
  }

  /** The lowest zoom a layer's chain has to reach, which is the lowest zoom it is queried at. */
  private static int bottom(MapLayer layer) {
    var queries = layer.getSourceQueries();
    var minzoom = queries.get(0).minzoom();
    return minzoom == null ? 1 : minzoom;
  }

  private static List<String> statements(String source, MapSpec.Generalize generalize, int bottom) {
    return LINES.equals(generalize.kind())
        ? lines(source, generalize, bottom)
        : areas(source, generalize, bottom);
  }

  private static List<String> areas(String source, MapSpec.Generalize generalize, int bottom) {
    var statements = new ArrayList<String>();
    // Highest zoom first: each level reads the one above it, so it has to exist already.
    for (int zoom = below(generalize) - 1; zoom >= bottom; zoom--) {
      var view = "%s_z%d".formatted(source, zoom);
      var from = zoom == below(generalize) - 1 ? source : "%s_z%d".formatted(source, zoom + 1);
      statements.add("DROP MATERIALIZED VIEW IF EXISTS %s CASCADE".formatted(view));
      statements.add(zoom >= mergeAbove(generalize)
          ? merged(view, from, zoom, generalize, zoom == below(generalize) - 1)
          : simplified(view, from, zoom, generalize));
      statements.add("CREATE INDEX IF NOT EXISTS %s_geom_idx ON %s USING GIST(geom)"
          .formatted(view, view));
      statements.add("CREATE INDEX IF NOT EXISTS %s_tags_idx ON %s USING GIN(tags)"
          .formatted(view, view));
    }
    return statements;
  }

  /**
   * Line work is merged once rather than level by level.
   *
   * <p>
   * Merging areas repeatedly is what makes neighbouring fields become a region; merging lines
   * repeatedly gains nothing, because two roads that meet are already one line after the first
   * pass. So the chain is one relation of merged line work, and every level simplifies out of it
   * and drops what is then too short to see. Which classes are worth a pixel at which zoom is the
   * judgement, and it is the only thing that changes between levels.
   */
  private static List<String> lines(String source, MapSpec.Generalize generalize, int bottom) {
    var statements = new ArrayList<String>();
    var merged = "%s_simplified".formatted(source);
    var key = literal(generalize.by());

    statements.add("DROP MATERIALIZED VIEW IF EXISTS %s CASCADE".formatted(merged));
    statements.add("""
        CREATE MATERIALIZED VIEW IF NOT EXISTS %s AS
        WITH filtered AS (
          SELECT tags -> %s AS tag, geom
          FROM %s
          WHERE tags ->> %s IN (%s)
        ),
        clustered AS (
          SELECT tag, geom, ST_ClusterDBSCAN(geom, 0, 1) OVER (PARTITION BY tag) AS cluster
          FROM filtered
        ),
        merged AS (
          SELECT tag, ST_LineMerge(ST_Collect(geom)) AS geom
          FROM clustered
          GROUP BY tag, cluster
        ),
        exploded AS (
          SELECT tag, (ST_Dump(geom)).geom AS geom FROM merged
        )
        SELECT ROW_NUMBER() OVER () AS id, jsonb_build_object(%s, tag) AS tags, geom
        FROM exploded"""
        .formatted(merged, key, source, key, values(generalize), key));
    statements.add("CREATE INDEX IF NOT EXISTS %s_geom ON %s USING GIST(geom)"
        .formatted(merged, merged));

    for (int zoom = below(generalize) - 1; zoom >= bottom; zoom--) {
      var view = "%s_z%d".formatted(source, zoom);
      statements.add("DROP MATERIALIZED VIEW IF EXISTS %s CASCADE".formatted(view));
      statements.add("""
          CREATE MATERIALIZED VIEW IF NOT EXISTS %s AS
          SELECT id, tags, st_simplifypreservetopology(geom, %s) AS geom
          FROM %s
          WHERE geom IS NOT NULL AND NOT ST_IsEmpty(geom) AND st_length(geom) > %s%s"""
          .formatted(view, tolerance(zoom), merged,
              "%s * %s".formatted(tolerance(zoom), number(length(generalize))),
              drawn(generalize, zoom)));
      statements.add("CREATE INDEX IF NOT EXISTS %s_geom_idx ON %s USING GIST(geom)"
          .formatted(view, view));
    }
    return statements;
  }

  /** The values still worth drawing at a zoom, when some stop before the bottom of the chain. */
  private static String drawn(MapSpec.Generalize generalize, int zoom) {
    var minzoom = generalize.minzoom();
    if (minzoom == null || minzoom.isEmpty()) {
      return "";
    }
    var drawn = generalize.values().stream()
        .filter(value -> zoom >= minzoom.getOrDefault(value, Integer.MIN_VALUE))
        .toList();
    // The merged relation holds these values and no others, so a test that keeps all of them keeps
    // everything and is only work.
    if (drawn.size() == generalize.values().size()) {
      return "";
    }
    return " AND tags ->> %s IN (%s)".formatted(literal(generalize.by()),
        drawn.stream().map(SchemaCompiler::literal).collect(Collectors.joining(", ")));
  }

  private static String values(MapSpec.Generalize generalize) {
    return generalize.values().stream().map(SchemaCompiler::literal)
        .collect(Collectors.joining(", "));
  }

  private static double length(MapSpec.Generalize generalize) {
    return value(generalize.length(), DEFAULT_LENGTH);
  }

  /**
   * Nearby areas of the same value are dilated until they touch, merged, eroded back by the same
   * amount and simplified.
   *
   * <p>
   * The area threshold is applied twice on purpose: once on the input, to keep small areas out of
   * the merge, and once on the result, to drop the slivers erosion leaves behind. Numbering the
   * rows after the dump rather than alongside it is what gives each part its own id; a window
   * function in the same select list as a set-returning function is evaluated before the rows are
   * expanded, so every part of a cluster would otherwise share one id.
   */
  private static String merged(String view, String from, int zoom, MapSpec.Generalize generalize,
      boolean select) {
    var tolerance = tolerance(zoom);
    var buffer = "%s * %s".formatted(tolerance, buffer(generalize));
    var area = area(zoom, generalize);
    var key = literal(generalize.by());

    // Only the first level of the chain chooses values; below it the ones left are already the ones
    // that were chosen. A layer that names none merges whatever it has.
    var chosen = generalize.values();
    var values = select && chosen != null && !chosen.isEmpty()
        ? " AND tags ->> %s IN (%s)".formatted(key,
            chosen.stream().map(SchemaCompiler::literal).collect(Collectors.joining(", ")))
        : "";

    return """
        CREATE MATERIALIZED VIEW IF NOT EXISTS %s AS
        WITH filtered AS (
          SELECT
            tags -> %s AS tag,
            st_buffer(st_simplifypreservetopology(geom, %s), %s, 'join=mitre') AS geom
          FROM %s
          WHERE geom IS NOT NULL AND NOT ST_IsEmpty(geom) AND st_area(geom) > %s%s
        ),
        clustered AS (
          SELECT tag, geom, st_clusterdbscan(geom, 0, 1) OVER (PARTITION BY tag) AS cluster
          FROM filtered
        ),
        merged AS (
          SELECT
            tag,
            st_simplifypreservetopology(
              (st_dump(st_buffer(st_collect(geom), - %s, 'join=mitre'))).geom, %s) AS geom
          FROM clustered
          GROUP BY tag, cluster
        )
        SELECT
          ROW_NUMBER() OVER () AS id,
          jsonb_build_object(%s, tag) AS tags,
          geom
        FROM merged
        WHERE geom IS NOT NULL AND NOT ST_IsEmpty(geom) AND st_area(geom) > %s"""
        .formatted(view, key, tolerance, buffer, from, area, values, buffer, tolerance, key, area);
  }

  /** Below the merge, the geometry is only simplified and the rows keep their identity. */
  private static String simplified(String view, String from, int zoom,
      MapSpec.Generalize generalize) {
    return """
        CREATE MATERIALIZED VIEW IF NOT EXISTS %s AS
        SELECT id, tags, st_simplifypreservetopology(geom, %s) AS geom
        FROM %s
        WHERE geom IS NOT NULL AND NOT ST_IsEmpty(geom) AND st_area(geom) > %s"""
        .formatted(view, tolerance(zoom), from, area(zoom, generalize));
  }

  private static String tolerance(int zoom) {
    return "%s / POWER(2, %d)".formatted(RESOLUTION, zoom);
  }

  private static String area(int zoom, MapSpec.Generalize generalize) {
    var threshold = zoom >= mergeAbove(generalize)
        ? value(generalize.area(), DEFAULT_AREA)
        : value(generalize.areaBelowMerge(), DEFAULT_AREA_BELOW_MERGE);
    return "POWER(%s, 2) * %s".formatted(tolerance(zoom), number(threshold));
  }

  private static int below(MapSpec.Generalize generalize) {
    return generalize.below() == null ? DEFAULT_BELOW : generalize.below();
  }

  private static int mergeAbove(MapSpec.Generalize generalize) {
    return generalize.mergeAbove() == null ? DEFAULT_MERGE_ABOVE : generalize.mergeAbove();
  }

  private static String buffer(MapSpec.Generalize generalize) {
    return number(value(generalize.buffer(), DEFAULT_BUFFER));
  }

  private static double value(Double declared, double fallback) {
    return declared == null ? fallback : declared;
  }

  /** Whole numbers are written without a decimal point, so the sql reads the way it was written. */
  private static String number(double value) {
    return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
  }

  private static String literal(String value) {
    return "'" + value.replace("'", "''") + "'";
  }
}
