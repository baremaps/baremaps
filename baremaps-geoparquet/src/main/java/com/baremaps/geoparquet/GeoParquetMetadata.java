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

package com.baremaps.geoparquet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;

/**
 * A representation of the metadata of a GeoParquet file, stored as JSON under the "geo" key of the
 * Parquet footer.
 *
 * @param version the version of the GeoParquet specification
 * @param primaryColumn the name of the primary geometry column
 * @param columns the metadata of the geometry columns, keyed by column name
 * @param encoding the encoding of the geometries
 * @param geometryTypes the geometry types found in the file
 * @param crs the coordinate reference system, as a PROJJSON object
 * @param edges the interpretation of the edges, either "planar" or "spherical"
 * @param bbox the bounding box of the file, as xmin, ymin, xmax, ymax
 * @param epoch the coordinate epoch
 * @param covering the columns covering the geometries, such as a bounding box group
 */
public record GeoParquetMetadata(
    @JsonProperty("version") String version,
    @JsonProperty("primary_column") String primaryColumn,
    @JsonProperty("columns") Map<String, Column> columns,
    @JsonProperty("encoding") String encoding,
    @JsonProperty("geometry_types") List<String> geometryTypes,
    @JsonProperty("crs") Object crs,
    @JsonProperty("edges") String edges,
    @JsonProperty("bbox") List<Double> bbox,
    @JsonProperty("epoch") String epoch,
    @JsonProperty("covering") Object covering) {

  /**
   * Unknown properties are ignored: the specification keeps growing and old readers must not break.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private static final String KEY = "geo";

  /**
   * The metadata of a geometry column.
   *
   * @param encoding the encoding of the geometries, such as "WKB"
   * @param geometryTypes the geometry types found in the column
   * @param crs the coordinate reference system, as a PROJJSON object
   * @param orientation the orientation of the polygon rings
   * @param edges the interpretation of the edges, either "planar" or "spherical"
   * @param bbox the bounding box of the column, as xmin, ymin, xmax, ymax
   */
  public record Column(
      @JsonProperty("encoding") String encoding,
      @JsonProperty("geometry_types") List<String> geometryTypes,
      @JsonProperty("crs") JsonNode crs,
      @JsonProperty("orientation") String orientation,
      @JsonProperty("edges") String edges,
      @JsonProperty("bbox") List<Double> bbox) {
  }

  /**
   * Reads the metadata from the key value metadata of a Parquet footer.
   *
   * @param keyValueMetadata the key value metadata of the file
   * @return the metadata, or null when the file is a plain Parquet file that declares none
   */
  public static GeoParquetMetadata read(Map<String, String> keyValueMetadata) {
    String json = keyValueMetadata.get(KEY);
    if (json == null) {
      return null;
    }
    try {
      return MAPPER.readValue(json, GeoParquetMetadata.class);
    } catch (JsonProcessingException e) {
      throw new GeoParquetException("Failed to parse the GeoParquet metadata of a file.", e);
    }
  }

  /**
   * Serializes the metadata for the key value metadata of a Parquet footer.
   *
   * @return the key value metadata holding the serialized metadata
   */
  public Map<String, String> write() {
    try {
      return Map.of(KEY, MAPPER.writeValueAsString(this));
    } catch (JsonProcessingException e) {
      throw new GeoParquetException("Failed to serialize the GeoParquet metadata.", e);
    }
  }

  /**
   * Returns the bounding box of the file as an envelope, or null when the file declares none. The
   * bounds are listed as xmin, ymin, xmax, ymax, whereas an envelope takes them as xmin, xmax,
   * ymin, ymax.
   *
   * @return the bounding box of the file
   */
  public Envelope envelope() {
    if (bbox == null || bbox.size() != 4) {
      return null;
    }
    return new Envelope(bbox.get(0), bbox.get(2), bbox.get(1), bbox.get(3));
  }
}
