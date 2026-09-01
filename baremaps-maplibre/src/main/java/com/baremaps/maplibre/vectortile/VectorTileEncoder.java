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

package com.baremaps.maplibre.vectortile;

import static com.baremaps.maplibre.vectortile.VectorTileFunctions.*;

import com.baremaps.maplibre.binary.VectorTile;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.locationtech.jts.geom.*;

/**
 * A vector tile encoder.
 *
 * This implementation is based on the Vector Tile Specification 2.1.
 */
public class VectorTileEncoder {

  private int cx = 0;

  private int cy = 0;

  private List<String> keys = new ArrayList<>();

  private List<Object> values = new ArrayList<>();

  /**
   * Constructs a vector tile encoder.
   */
  public VectorTileEncoder() {
    // Default constructor
  }

  /**
   * Encodes a tile into a vector tile.
   * 
   * @param tile The tile to encode
   * @return The vector tile
   */
  public VectorTile.Tile encodeTile(Tile tile) {
    VectorTile.Tile.Builder builder = VectorTile.Tile.newBuilder();
    tile.getLayers().forEach(layer -> builder.addLayers(encodeLayer(layer)));
    return builder.build();
  }

  /**
   * Encodes a layer into a vector tile layer.
   * 
   * @param layer The layer to encode
   * @return The vector tile layer
   */
  public VectorTile.Tile.Layer encodeLayer(Layer layer) {
    keys = new ArrayList<>();
    values = new ArrayList<>();

    VectorTile.Tile.Layer.Builder builder = VectorTile.Tile.Layer.newBuilder();
    builder.setName(layer.getName());
    builder.setVersion(2);
    builder.setExtent(layer.getExtent());

    // Encode the features
    layer.getFeatures().stream()
        .forEach(feature -> encodeFeature(feature, builder::addFeatures));

    // Encode the keys and values
    builder.addAllKeys(keys);
    builder.addAllValues(values.stream().map(this::encodeValue).toList());

    return builder.build();
  }

  /**
   * Encodes a Java object into a vector tile value.
   * 
   * @param object The object to encode
   * @return The vector tile value
   */
  protected VectorTile.Tile.Value encodeValue(Object object) {
    VectorTile.Tile.Value.Builder builder = VectorTile.Tile.Value.newBuilder();

    // Encode the value based on its type
    if (object instanceof String value) {
      builder.setStringValue(value);
    } else if (object instanceof Float value) {
      builder.setFloatValue(value);
    } else if (object instanceof Double value) {
      builder.setDoubleValue(value);
    } else if (object instanceof Integer value) {
      builder.setIntValue(value);
    } else if (object instanceof Long value) {
      builder.setIntValue(value);
    } else if (object instanceof Boolean value) {
      builder.setBoolValue(value);
    }

    return builder.build();
  }

  /**
   * Encode a feature.
   *
   * @param feature The feature to encode.
   */
  protected void encodeFeature(Feature feature, Consumer<VectorTile.Tile.Feature> consumer) {
    cx = 0;
    cy = 0;

    VectorTile.Tile.Feature.Builder builder = VectorTile.Tile.Feature.newBuilder();
    builder.setId(feature.getId());
    builder.setType(encodeGeometryType(feature.getGeometry()));

    encodeTag(feature.getTags(), builder::addTags);
    encodeGeometry(feature.getGeometry(), builder::addGeometry);

    consumer.accept(builder.build());
  }

  protected VectorTile.Tile.GeomType encodeGeometryType(Geometry geometry) {
    if (geometry instanceof Point) {
      return VectorTile.Tile.GeomType.POINT;
    } else if (geometry instanceof MultiPoint) {
      return VectorTile.Tile.GeomType.POINT;
    } else if (geometry instanceof LineString) {
      return VectorTile.Tile.GeomType.LINESTRING;
    } else if (geometry instanceof MultiLineString) {
      return VectorTile.Tile.GeomType.LINESTRING;
    } else if (geometry instanceof Polygon) {
      return VectorTile.Tile.GeomType.POLYGON;
    } else if (geometry instanceof MultiPolygon) {
      return VectorTile.Tile.GeomType.POLYGON;
    } else {
      return VectorTile.Tile.GeomType.UNKNOWN;
    }
  }


  /**
   * Encode the tags of a feature.
   *
   * @param tags The tags of a feature.
   * @param encoding The consumer of the tags.
   */
  protected void encodeTag(Map<String, Object> tags, IntConsumer encoding) {
    for (Entry<String, Object> tag : tags.entrySet()) {
      int keyIndex = keys.indexOf(tag.getKey());
      if (keyIndex == -1) {
        keyIndex = keys.size();
        keys.add(tag.getKey());
      }
      int valueIndex = values.indexOf(tag.getValue());
      if (valueIndex == -1) {
        valueIndex = values.size();
        values.add(tag.getValue());
      }
      encoding.accept(keyIndex);
      encoding.accept(valueIndex);
    }
  }

  /**
   * Encode a geometry into a list of commands and parameters.
   *
   * @param geometry The geometry to encode.
   * @param encoding The consumer of commands and parameters.
   */
  protected void encodeGeometry(Geometry geometry, IntConsumer encoding) {
    if (geometry instanceof Point point) {
      encodePoint(point, encoding);
    } else if (geometry instanceof MultiPoint multiPoint) {
      encodeMultiPoint(multiPoint, encoding);
    } else if (geometry instanceof LineString lineString) {
      encodeLineString(lineString, encoding);
    } else if (geometry instanceof MultiLineString multiLineString) {
      encodeMultiLineString(multiLineString, encoding);
    } else if (geometry instanceof Polygon polygon) {
      encodePolygon(polygon, encoding);
    } else if (geometry instanceof MultiPolygon multiPolygon) {
      encodeMultiPolygon(multiPolygon, encoding);
    } else if (geometry instanceof GeometryCollection) {
      throw new UnsupportedOperationException("GeometryCollection not supported");
    }
  }

  /**
   * Encodes a point into a list of commands and parameters.
   *
   * @param point The point to encode.
   * @param encoding The consumer of commands and parameters.
   */
  protected void encodePoint(Point point, IntConsumer encoding) {
    encoding.accept(command(MOVE_TO, 1));
    Coordinate coordinate = point.getCoordinate();
    int dx = (int) Math.round(coordinate.getX()) - cx;
    int dy = (int) Math.round(coordinate.getY()) - cy;
    encoding.accept(parameter(dx));
    encoding.accept(parameter(dy));
    cx += dx;
    cy += dy;
  }

  /**
   * Encodes a multipoint into a list of commands and parameters.
   * 
   * @param multiPoint The multipoint to encode.
   * @param encoding The consumer of commands and parameters.
   */
  protected void encodeMultiPoint(MultiPoint multiPoint, IntConsumer encoding) {
    List<Coordinate> coordinates = List.of(multiPoint.getCoordinates());
    encoding.accept(command(MOVE_TO, coordinates.size()));
    encodeCoordinates(coordinates, encoding);
  }

  /**
   * Encodes a linestring into a list of commands and parameters.
   * 
   * @param lineString The linestring to encode.
   * @param encoding The consumer of commands and parameters.
   */
  protected void encodeLineString(LineString lineString, IntConsumer encoding) {
    List<Coordinate> coordinates = List.of(lineString.getCoordinates());
    encoding.accept(command(MOVE_TO, 1));
    encodeCoordinates(coordinates.subList(0, 1), encoding);
    encoding.accept(command(LINE_TO, coordinates.size() - 1));
    encodeCoordinates(coordinates.subList(1, coordinates.size()), encoding);
  }

  /**
   * Encodes a multilinestring into a list of commands and parameters.
   * 
   * @param multiLineString The multilinestring to encode.
   * @param encoding The consumer of commands and parameters.
   */
  protected void encodeMultiLineString(MultiLineString multiLineString, IntConsumer encoding) {
    for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
      Geometry geometry = multiLineString.getGeometryN(i);
      if (geometry instanceof LineString lineString) {
        encodeLineString(lineString, encoding);
      }
    }
  }

  /**
   * Encodes a polygon into a list of commands and parameters.
   * 
   * @param polygon The polygon to encode.
   * @param encoding The consumer of commands and parameters.
   */
  protected void encodePolygon(Polygon polygon, IntConsumer encoding) {
    // Every ring is rounded to the tile grid before its winding is decided, because the winding is
    // the only thing that tells a decoder an outline from a hole, and it is the rounded ring the
    // decoder sees. A thin ring can wind one way at full precision and the other way once its
    // vertices land on the grid: judged before rounding, such an outline is written unreversed and
    // read back as a hole, leaving a polygon that has holes and no outline at all.
    List<Coordinate> exteriorRingCoordinates = rounded(polygon.getExteriorRing());

    // Exterior ring must be clockwise
    if (!isClockWise(exteriorRingCoordinates)) {
      exteriorRingCoordinates = Lists.reverse(exteriorRingCoordinates);
    }

    encodeRing(exteriorRingCoordinates, encoding);

    for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
      List<Coordinate> interiorRingCoordinates = rounded(polygon.getInteriorRingN(i));

      // Interior rings must be counter-clockwise, which is what tells a decoder they are holes.
      if (isClockWise(interiorRingCoordinates)) {
        interiorRingCoordinates = Lists.reverse(interiorRingCoordinates);
      }

      encodeRing(interiorRingCoordinates, encoding);
    }
  }

  /** A ring's coordinates on the tile grid, rounded the way {@link #encodeCoordinates} rounds. */
  private static List<Coordinate> rounded(LinearRing ring) {
    var coordinates = new ArrayList<Coordinate>(ring.getNumPoints());
    for (var coordinate : ring.getCoordinates()) {
      coordinates.add(new Coordinate(Math.round(coordinate.getX()), Math.round(coordinate.getY())));
    }
    return coordinates;
  }

  /** Whether a ring of already rounded coordinates winds clockwise on the tile grid. */
  private static boolean isClockWise(List<Coordinate> coordinates) {
    return VectorTileFunctions.isClockWise(coordinates.toArray(new Coordinate[0]));
  }

  /**
   * Encodes a ring into a list of commands and parameters.
   *
   * @param coordinates The coordinates of the ring
   * @param encoding The consumer of commands and parameters
   */
  protected void encodeRing(List<Coordinate> coordinates, IntConsumer encoding) {
    // Move to first point
    List<Coordinate> head = coordinates.subList(0, 1);
    encoding.accept(command(MOVE_TO, 1));
    encodeCoordinates(head, encoding);

    // Line to remaining points
    List<Coordinate> tail = coordinates.subList(1, coordinates.size() - 1);
    encoding.accept(command(LINE_TO, tail.size()));
    encodeCoordinates(tail, encoding);

    // Close the ring
    encoding.accept(command(CLOSE_PATH, 1));
  }

  /**
   * Encodes a multipolygon into a list of commands and parameters.
   * 
   * @param multiPolygon The multipolygon to encode
   * @param encoding The consumer of commands and parameters
   */
  protected void encodeMultiPolygon(MultiPolygon multiPolygon, IntConsumer encoding) {
    for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
      Geometry geometry = multiPolygon.getGeometryN(i);
      if (geometry instanceof Polygon polygon) {
        encodePolygon(polygon, encoding);
      }
    }
  }

  /**
   * Encodes a list of coordinates into a list of parameters.
   * 
   * @param coordinates The coordinates to encode
   * @param encoding The consumer of parameters
   */
  protected void encodeCoordinates(List<Coordinate> coordinates, IntConsumer encoding) {
    for (Coordinate coordinate : coordinates) {
      int dx = (int) Math.round(coordinate.getX()) - cx;
      int dy = (int) Math.round(coordinate.getY()) - cy;
      encoding.accept(parameter(dx));
      encoding.accept(parameter(dy));
      cx += dx;
      cy += dy;
    }
  }

  /**
   * Encodes a command.
   *
   * @param id The command id
   * @param count The number of parameters
   * @return The encoded command
   */
  protected static int command(int id, int count) {
    return (id & 0x7) | (count << 3);
  }

  /**
   * Encodes a parameter.
   *
   * @param value The parameter value
   * @return The encoded parameter
   */
  protected static int parameter(int value) {
    return (value << 1) ^ (value >> 31);
  }
}
