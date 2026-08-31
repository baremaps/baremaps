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


import com.baremaps.maplibre.expression.Expressions;
import com.baremaps.maplibre.expression.Expressions.Equal;
import com.baremaps.maplibre.expression.Expressions.Expression;
import com.baremaps.maplibre.expression.Expressions.Get;
import com.baremaps.maplibre.expression.Expressions.Has;
import com.baremaps.maplibre.expression.Expressions.Interpolate;
import com.baremaps.maplibre.expression.Expressions.Literal;
import com.baremaps.maplibre.expression.Expressions.Step;
import com.baremaps.maplibre.expression.Expressions.Zoom;
import com.baremaps.maplibre.style.Style;
import com.baremaps.maplibre.style.StyleLayer;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * What a style requires of the tiles it is drawn from, at one zoom level.
 *
 * <p>
 * A style names the attributes it reads and the zoom levels at which it draws. A tileset that ships
 * more than that pays for it in every tile, and one that ships less loses features. Deriving the
 * first from the second keeps the two in step, which hand maintenance does not: the basemap this
 * was written for shipped 922 distinct attributes on a layer whose style reads 40.
 *
 * <p>
 * Two rules make the answer safe to act on.
 *
 * <p>
 * The analysis widens rather than narrows. An expression that is not understood contributes its
 * attributes as unbounded rather than contributing nothing, so an unmodelled construct costs bytes
 * and never costs features. Silently answering "reads nothing" would produce a tileset that omits
 * the attributes a layer filters on, which renders as absence rather than as an error.
 *
 * <p>
 * Zoom is a half open interval, not a point. A tile at zoom z is drawn for map zooms z up to z+1,
 * so a layer is required at z when it draws anywhere in that span. Evaluating at the integer alone
 * is how a layer gets dropped that the map still needs: the basemap fades buildings in from an
 * opacity of zero at 13 to one at 13.5, so buildings are invisible at exactly 13 and essential
 * immediately after, and both are served by the z13 tile.
 */
public final class Demand {

  /**
   * Paint and layout properties that hide a layer when they reach zero. A layer whose size, width
   * or opacity is zero across a whole zoom span draws nothing there, whatever its filter says.
   */
  private static final List<String> VANISHING = List.of(
      "fill-opacity",
      "line-opacity",
      "line-width",
      "text-opacity",
      "text-size",
      "icon-opacity",
      "icon-size",
      "fill-extrusion-opacity",
      "fill-extrusion-height");

  private static final double MAX_ZOOM = 24;

  private Demand() {}

  /**
   * The attributes one source layer must carry.
   *
   * @param keys every attribute the style reads
   * @param values the values compared against, for those attributes compared only against literals;
   *        an attribute absent from this map is read in a way that admits any value
   */
  public record Attributes(SortedSet<String> keys, SortedMap<String, SortedSet<String>> values) {

    public boolean isBounded(String key) {
      return values.containsKey(key);
    }
  }

  /**
   * Returns what each source layer must carry for tiles at the given zoom, keyed by source layer. A
   * source layer no style layer draws at this zoom is absent.
   *
   * @param style the style
   * @param zoom the zoom level of the tile
   * @return the attributes required of each source layer
   */
  public static SortedMap<String, Attributes> of(Style style, int zoom) {
    var builders = new TreeMap<String, Builder>();
    for (var layer : style.getLayers()) {
      var source = layer.getSourceLayer();
      if (source == null || !draws(layer, zoom)) {
        continue;
      }
      var builder = builders.computeIfAbsent(source, key -> new Builder());
      collect(expression(layer.getFilter()), builder);
      for (var value : properties(layer.getLayout()).values()) {
        collect(expression(value), builder);
      }
      for (var value : properties(layer.getPaint()).values()) {
        collect(expression(value), builder);
      }
    }
    var demand = new TreeMap<String, Attributes>();
    builders.forEach((source, builder) -> demand.put(source, builder.build()));
    return demand;
  }

  /**
   * Returns whether a style layer draws anything on a tile at the given zoom, which is to say
   * anywhere in the span that tile serves.
   *
   * @param layer the style layer
   * @param zoom the zoom level of the tile
   * @return whether the layer draws
   */
  public static boolean draws(StyleLayer layer, int zoom) {
    if (hidden(layer)) {
      return false;
    }

    // The span of map zooms this tile is drawn for, narrowed to where the layer is enabled.
    double from = zoom;
    double to = zoom + 1d;
    double minzoom = layer.getMinzoom() == null ? 0 : layer.getMinzoom();
    double maxzoom = layer.getMaxzoom() == null ? MAX_ZOOM : layer.getMaxzoom();
    if (to <= minzoom || from >= maxzoom) {
      return false;
    }
    from = Math.max(from, minzoom);
    to = Math.min(to, maxzoom);

    for (var property : VANISHING) {
      var value = properties(layer.getPaint()).get(property);
      if (value == null) {
        value = properties(layer.getLayout()).get(property);
      }
      if (value == null) {
        continue;
      }
      var bounds = bounds(expression(value), from, to);
      // A property that cannot be bounded is assumed to draw.
      if (bounds != null && bounds.max() == 0) {
        return false;
      }
    }
    return true;
  }

  private static boolean hidden(StyleLayer layer) {
    return "none".equals(properties(layer.getLayout()).get("visibility"));
  }

  // --- attributes ----------------------------------------------------------

  private static void collect(Expression expression, Builder builder) {
    if (expression == null) {
      return;
    }

    // An equality against a literal is the one shape that bounds the values an attribute may take,
    // and the only one a generated query can turn into an IN clause. Every other shape, an
    // inequality or a match with a fallback among them, leaves the attribute unbounded.
    if (expression instanceof Equal(Expression left, Expression right)) {
      var bound = bind(left, right, builder) || bind(right, left, builder);
      if (bound) {
        return;
      }
    }

    if (expression instanceof Get(String property)) {
      builder.unbounded(property);
      return;
    }
    if (expression instanceof Has(String property)) {
      builder.unbounded(property);
      return;
    }

    for (var child : arguments(expression)) {
      collect(child, builder);
    }
  }

  private static boolean bind(Expression attribute, Expression value, Builder builder) {
    if (attribute instanceof Get(String property) && value instanceof Literal(Object literal)
        && literal != null) {
      builder.bounded(property, String.valueOf(literal));
      return true;
    }
    return false;
  }

  // --- zoom ----------------------------------------------------------------

  private record Bounds(double min, double max) {
  }

  /**
   * The range a zoom driven expression spans over the half open interval, or null when it cannot be
   * determined, which the caller reads as "assume it draws".
   */
  private static Bounds bounds(Expression expression, double from, double to) {
    if (expression instanceof Literal(Object value) && value instanceof Number number) {
      return new Bounds(number.doubleValue(), number.doubleValue());
    }
    if (expression instanceof Interpolate(Expression ignored, Expression input,
        List<Expression> stops)) {
      return interpolated(input, stops, from, to);
    }
    if (expression instanceof Step(Expression input, Expression fallback,
        List<Expression> stops)) {
      return stepped(input, fallback, stops, from, to);
    }
    return null;
  }

  private static Bounds interpolated(Expression input, List<Expression> stops, double from,
      double to) {
    if (!(input instanceof Zoom)) {
      return null;
    }
    var points = points(stops);
    if (points == null || points.isEmpty()) {
      return null;
    }
    // Interpolation is piecewise monotonic between stops, so the extremes over an interval are
    // reached at its ends or at a stop inside it.
    var probes = probes(points, from, to);
    var values = new ArrayList<Double>();
    for (var probe : probes) {
      values.add(interpolate(points, probe));
    }
    return new Bounds(Collections.min(values), Collections.max(values));
  }

  private static Bounds stepped(Expression input, Expression fallback, List<Expression> stops,
      double from, double to) {
    if (!(input instanceof Zoom) || !(fallback instanceof Literal(Object first))
        || !(first instanceof Number)) {
      return null;
    }
    var points = points(stops);
    if (points == null) {
      return null;
    }
    var values = new ArrayList<Double>();
    for (var probe : probes(points, from, to)) {
      double value = ((Number) first).doubleValue();
      for (var point : points) {
        if (probe >= point[0]) {
          value = point[1];
        }
      }
      values.add(value);
    }
    return values.isEmpty() ? null : new Bounds(Collections.min(values), Collections.max(values));
  }

  /** The zoom levels at which an interval has to be sampled: its ends and any stop within it. */
  private static List<Double> probes(List<double[]> points, double from, double to) {
    var probes = new ArrayList<Double>();
    probes.add(from);
    for (var point : points) {
      if (point[0] > from && point[0] < to) {
        probes.add(point[0]);
      }
    }
    // The interval is half open, so its upper end is approached but never reached.
    probes.add(Math.nextDown(to));
    return probes;
  }

  /** Reads alternating input and output literals, or null if any of them is not a number. */
  private static List<double[]> points(List<Expression> stops) {
    if (stops.size() % 2 != 0) {
      return null;
    }
    var points = new ArrayList<double[]>();
    for (int i = 0; i < stops.size(); i += 2) {
      if (!(stops.get(i) instanceof Literal(Object input)) || !(input instanceof Number in)
          || !(stops.get(i + 1) instanceof Literal(Object output))
          || !(output instanceof Number out)) {
        return null;
      }
      points.add(new double[] {in.doubleValue(), out.doubleValue()});
    }
    points.sort((left, right) -> Double.compare(left[0], right[0]));
    return points;
  }

  private static double interpolate(List<double[]> points, double zoom) {
    var first = points.get(0);
    var last = points.get(points.size() - 1);
    if (zoom <= first[0]) {
      return first[1];
    }
    if (zoom >= last[0]) {
      return last[1];
    }
    for (int i = 0; i < points.size() - 1; i++) {
      var low = points.get(i);
      var high = points.get(i + 1);
      if (zoom >= low[0] && zoom <= high[0]) {
        if (high[0] == low[0]) {
          return high[1];
        }
        // Linear between the stops. An exponential base makes the curve sag between them but keeps
        // the same endpoints, so the extremes of the interval are unchanged.
        return low[1] + (high[1] - low[1]) * (zoom - low[0]) / (high[0] - low[0]);
      }
    }
    return last[1];
  }

  // --- plumbing ------------------------------------------------------------

  private static Expression expression(Object value) {
    if (value == null) {
      return null;
    }
    return Expressions.from(value);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> properties(Object value) {
    if (value instanceof Map<?, ?>map) {
      return (Map<String, Object>) map;
    }
    return Map.of();
  }

  /**
   * The sub expressions of an expression, read from its record components so that an expression
   * added later, and an unknown one, are traversed without being enumerated here.
   */
  private static List<Expression> arguments(Expression expression) {
    var components = expression.getClass().getRecordComponents();
    if (components == null) {
      return List.of();
    }
    var arguments = new ArrayList<Expression>();
    for (RecordComponent component : components) {
      try {
        var value = component.getAccessor().invoke(expression);
        if (value instanceof Expression child) {
          arguments.add(child);
        } else if (value instanceof List<?>list) {
          for (var element : list) {
            if (element instanceof Expression child) {
              arguments.add(child);
            }
          }
        }
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException("Cannot read the arguments of " + expression.name(), e);
      }
    }
    return arguments;
  }

  private static final class Builder {

    private final SortedSet<String> keys = new TreeSet<>();
    private final SortedMap<String, SortedSet<String>> values = new TreeMap<>();
    private final Set<String> unbounded = new HashSet<>();

    void bounded(String key, String value) {
      keys.add(key);
      if (!unbounded.contains(key)) {
        values.computeIfAbsent(key, ignored -> new TreeSet<>()).add(value);
      }
    }

    void unbounded(String key) {
      keys.add(key);
      unbounded.add(key);
      values.remove(key);
    }

    Attributes build() {
      return new Attributes(Collections.unmodifiableSortedSet(keys),
          Collections.unmodifiableSortedMap(values));
    }
  }
}
