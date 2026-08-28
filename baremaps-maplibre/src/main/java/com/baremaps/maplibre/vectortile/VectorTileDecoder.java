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
import com.baremaps.maplibre.binary.VectorTile.Tile.GeomType;
import com.baremaps.maplibre.binary.VectorTile.Tile.Value;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import org.locationtech.jts.geom.*;

/**
 * A vector tile decoder.
 *
 * This implementation is based on the Vector Tile Specification.
 */
public class VectorTileDecoder {
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  private int cx = 0;

  private int cy = 0;

  private List<String> keys = new ArrayList<>();

  private List<Object> values = new ArrayList<>();

  /**
   * Constructs a new vector tile decoder.
   */
  public VectorTileDecoder() {
    // Default constructor
  }

  /**
   * Decodes a vector tile.
   *
   * @param buffer The bytes of the vector tile
   * @return The decoded vector tile
   * @throws IOException If an error occurs while decoding the vector tile
   */
  public Tile decodeTile(ByteBuffer buffer) throws IOException {
    try {
      VectorTile.Tile tile = VectorTile.Tile.parseFrom(buffer);
      return decodeTile(tile);
    } catch (InvalidProtocolBufferException e) {
      throw new IOException(e);
    }
  }

  /**
   * Decodes a vector tile.
   *
   * @param tile The vector tile to decode
   * @return The decoded vector tile
   */
  public Tile decodeTile(VectorTile.Tile tile) {
    List<Layer> layers = tile.getLayersList().stream()
        .map(this::decodeLayer)
        .toList();
    return new Tile(layers);
  }

  /**
   * Decodes a vector tile layer.
   *
   * @param buffer The bytes of the vector tile layer
   * @return The decoded layer
   * @throws IOException If an error occurs while decoding the vector tile layer
   */
  public Layer decodeLayer(ByteBuffer buffer) throws IOException {
    try {
      VectorTile.Tile.Layer layer = VectorTile.Tile.Layer.parseFrom(buffer);
      return decodeLayer(layer);
    } catch (InvalidProtocolBufferException e) {
      throw new IOException(e);
    }
  }

  /**
   * Decodes a vector tile layer.
   *
   * @param layer The vector tile layer
   * @return The decoded layer
   */
  public Layer decodeLayer(VectorTile.Tile.Layer layer) {
    String name = layer.getName();
    int extent = layer.getExtent();

    cx = 0;
    cy = 0;

    keys = layer.getKeysList();
    values = layer.getValuesList().stream()
        .map(this::decodeValue)
        .toList();

    List<Feature> features = layer.getFeaturesList().stream()
        .map(this::decodeFeature)
        .toList();

    return new Layer(name, extent, features);
  }

  /**
   * Decodes a vector tile value into a Java object.
   *
   * @param value The vector tile value
   * @return The Java object
   */
  protected Object decodeValue(Value value) {
    if (value.hasStringValue()) {
      return value.getStringValue();
    } else if (value.hasFloatValue()) {
      return value.getFloatValue();
    } else if (value.hasDoubleValue()) {
      return value.getDoubleValue();
    } else if (value.hasIntValue()) {
      return value.getIntValue();
    } else if (value.hasSintValue()) {
      return value.getSintValue();
    } else if (value.hasUintValue()) {
      return value.getUintValue();
    } else if (value.hasBoolValue()) {
      return value.getBoolValue();
    } else {
      throw new IllegalStateException("Value is not set.");
    }
  }

  /**
   * Decodes a feature from a vector tile.
   * 
   * @param feature The vector tile feature to decode
   * @return The decoded feature
   */
  protected Feature decodeFeature(VectorTile.Tile.Feature feature) {

    cx = 0;
    cy = 0;

    long id = feature.getId();
    Map<String, Object> tags = decodeTags(feature);
    Geometry geometry = decodeGeometry(feature);
    return new Feature(id, tags, geometry);
  }

  /**
   * Decodes the tags from a vector tile feature.
   *
   * @param feature The feature to decode
   * @return The tags of the feature
   */
  protected Map<String, Object> decodeTags(VectorTile.Tile.Feature feature) {
    Map<String, Object> tags = new HashMap<>();
    List<Integer> encoding = feature.getTagsList();
    for (int i = 0; i < encoding.size(); i += 2) {
      int key = encoding.get(i);
      int value = encoding.get(i + 1);
      tags.put(keys.get(key), values.get(value));
    }
    return tags;
  }

  /**
   * Decodes a geometry from a vector tile feature.
   *
   * @param feature The vector tile feature
   * @return The decoded geometry
   */
  protected Geometry decodeGeometry(VectorTile.Tile.Feature feature) {
    GeomType type = feature.getType();
    List<Integer> encoding = feature.getGeometryList();
    switch (type) {
      case POINT:
        return decodePoint(encoding);
      case LINESTRING:
        return decodeLineString(encoding);
      case POLYGON:
        return decodePolygon(encoding);
      case UNKNOWN:
      default:
        throw new IllegalStateException("Unknown geometry type.");
    }
  }

  /**
   * Decode a point geometry.
   *
   * @param encoding The encoding of the point geometry
   * @return The decoded point geometry
   */
  @SuppressWarnings("squid:S3776")
  protected Geometry decodePoint(List<Integer> encoding) {
    List<Coordinate> coordinates = new ArrayList<>();

    // Iterate over the commands and parameters
    int i = 0;
    while (i < encoding.size()) {
      int value = encoding.get(i);
      int command = command(value);
      int count = count(value);

      // Increment the index to the first parameter
      i++;

      // Iterate over the parameters
      int length = count * 2;
      for (int j = 0; j < length; j += 2) {
        // Decode the parameters and move the cursor
        cx += parameter(encoding.get(i + j));
        cy += parameter(encoding.get(i + j + 1));

        // Add the coordinate to the list
        if (command == MOVE_TO) {
          coordinates.add(new Coordinate(cx, cy));
        }
      }

      // Increment the index to the next command
      i += length;
    }

    // Build the final geometry
    if (coordinates.size() == 1) {
      return GEOMETRY_FACTORY.createPoint(coordinates.get(0));
    } else if (coordinates.size() > 1) {
      return GEOMETRY_FACTORY.createMultiPointFromCoords(coordinates.toArray(new Coordinate[0]));
    } else {
      throw new IllegalStateException("No coordinates found.");
    }
  }

  /**
   * Decode a line string.
   *
   * @param encoding The encoding of the line string
   * @return The decoded line string
   */
  protected Geometry decodeLineString(List<Integer> encoding) {
    List<LineString> lineStrings = new ArrayList<>();
    List<Coordinate> coordinates = new ArrayList<>();

    // Iterate over the commands and parameters
    int i = 0;
    while (i < encoding.size()) {
      int value = encoding.get(i);
      int command = command(value);
      int count = count(value);

      // Increment the index to the first parameter
      i++;

      // Iterate over the parameters
      int length = count * 2;
      for (int j = 0; j < length; j += 2) {
        // Decode the parameters and move the cursor
        cx += parameter(encoding.get(i + j));
        cy += parameter(encoding.get(i + j + 1));

        // Start a new linestring
        if (command == MOVE_TO) {
          coordinates.clear();
          coordinates.add(new Coordinate(cx, cy));
        }

        // Add the coordinate to the current linestring
        else if (command == LINE_TO) {
          coordinates.add(new Coordinate(cx, cy));
        }
      }

      // Add the linestring to the list of linestrings
      if (coordinates.size() > 1) {
        lineStrings.add(GEOMETRY_FACTORY.createLineString(coordinates.toArray(new Coordinate[0])));
      }

      // Increment the index to the next command
      i += length;
    }

    // Build the final geometry
    if (lineStrings.size() == 1) {
      return lineStrings.get(0);
    } else if (lineStrings.size() > 1) {
      return GEOMETRY_FACTORY.createMultiLineString(lineStrings.toArray(new LineString[0]));
    } else {
      throw new IllegalStateException("No linestrings found.");
    }
  }

  /**
   * Decodes a polygon geometry.
   *
   * @param encoding The encoding of the polygon
   * @return The geometry
   */
  protected Geometry decodePolygon(List<Integer> encoding) {
    List<Polygon> polygons = new ArrayList<>();
    Optional<LinearRing> shell = Optional.empty();
    List<LinearRing> holes = new ArrayList<>();
    List<Coordinate> coordinates = new ArrayList<>();

    // Iterate over the commands and parameters
    int i = 0;
    while (i < encoding.size()) {
      int value = encoding.get(i);
      int command = command(value);
      int count = count(value);

      // Accumulate the coordinates
      if (command == MOVE_TO || command == LINE_TO) {

        // Increment the index to the first parameter
        i++;

        int length = count * 2;
        for (int j = 0; j < length; j += 2) {
          // Decode the parameters and move the cursor
          cx += parameter(encoding.get(i + j));
          cy += parameter(encoding.get(i + j + 1));

          // Start a new linear ring
          if (command == MOVE_TO) {
            coordinates.clear();
            coordinates.add(new Coordinate(cx, cy));
          }

          // Add the coordinate to the current linear ring
          else if (command == LINE_TO) {
            coordinates.add(new Coordinate(cx, cy));
          }
        }

        // Increment the index to the next command
        i += length;
      }

      // Assemble the linear rings
      if (command == CLOSE_PATH) {
        coordinates.add(coordinates.get(0));
        LinearRing linearRing =
            GEOMETRY_FACTORY.createLinearRing(coordinates.toArray(new Coordinate[0]));
        boolean isShell = isClockWise(linearRing);

        // Build the previous polygon
        if (isShell && shell.isPresent()) {
          polygons
              .add(GEOMETRY_FACTORY.createPolygon(shell.get(), holes.toArray(new LinearRing[0])));
          holes.clear();
        }

        // Add the linear ring to the appropriate variable
        if (isShell) {
          shell = Optional.of(linearRing);
        } else {
          holes.add(linearRing);
        }

        // Reset the coordinates
        coordinates.clear();

        // Increment the index to the next command
        i++;
      }
    }

    // Build the last polygon
    if (shell.isPresent()) {
      polygons.add(GEOMETRY_FACTORY.createPolygon(shell.get(), holes.toArray(new LinearRing[0])));
      holes.clear();
    }

    // Build the final geometry
    if (polygons.size() == 1) {
      return polygons.get(0);
    } else if (polygons.size() > 1) {
      return GEOMETRY_FACTORY.createMultiPolygon(polygons.toArray(new Polygon[0]));
    } else {
      throw new IllegalStateException("No polygons found.");
    }
  }

  /**
   * Returns the command for the given value.
   *
   * @param value The value
   * @return The command
   */
  protected int command(int value) {
    return value & 0x7;
  }

  /**
   * Returns the number of parameters for the given value.
   *
   * @param value The value
   * @return The number of parameters
   */
  protected int count(int value) {
    return value >> 3;
  }

  /**
   * Decodes a parameter from the given value.
   *
   * @param value The value to decode
   * @return The decoded parameter
   */
  protected Integer parameter(int value) {
    return (value >> 1) ^ (-(value & 1));
  }
}
