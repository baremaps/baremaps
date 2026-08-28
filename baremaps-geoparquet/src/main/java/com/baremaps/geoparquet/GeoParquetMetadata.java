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
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

/**
 * A representation of the metadata of a GeoParquet file encoded as JSON.
 *
 * @param version
 * @param primaryColumn
 * @param columns
 * @param encoding
 * @param geometryTypes
 * @param crs
 * @param edges
 * @param bbox
 * @param epoch
 * @param covering
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
   * A representation of the metadata of a column encoded as JSON.
   *
   * @param encoding
   * @param geometryTypes
   * @param crs
   * @param orientation
   * @param edges
   * @param bbox
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
   * Returns the SRID of the specified column.
   *
   * @param column the column
   * @return the SRID, or 4326 if no valid SRID is found
   */
  public int getSrid(String column) {
    // Retrieve the column metadata, return default SRID if not present
    Column columnMetadata = columns.get(column);
    if (columnMetadata == null || columnMetadata.crs() == null) {
      return 4326; // Default to 4326 if no CRS is present
    }

    // Extract the CRS JsonNode
    JsonNode crsNode = columnMetadata.crs();
    JsonNode idNode = crsNode.get("id");

    // Return default SRID if "id" field is not present
    if (idNode == null || idNode.get("authority") == null || idNode.get("code") == null) {
      return 4326;
    }

    // Extract authority and code values
    String authority = idNode.get("authority").asText();
    String code = idNode.get("code").asText();

    // Determine SRID based on authority and code
    if (authority.equals("EPSG")) {
      return getEpsgCode(code);
    } else if (authority.equals("OGC")) {
      return getOgcSrid(code);
    } else {
      return 4326; // Default SRID if authority is unrecognized
    }
  }

  /**
   * Handle OGC-specific SRIDs.
   *
   * @param code the OGC code
   * @return the SRID, or 0 if the code is unrecognized
   */
  private int getOgcSrid(String code) {
    if ("CRS84".equals(code)) {
      return 4326;
    } else {
      return 0; // Unrecognized OGC code
    }
  }

  /**
   * Parse EPSG SRID code as an integer.
   *
   * @param code the EPSG code
   * @return the SRID as an integer, or 0 if the code is invalid
   */
  private int getEpsgCode(String code) {
    try {
      return Integer.parseInt(code);
    } catch (NumberFormatException e) {
      return 0; // Return 0 if the code is not a valid integer
    }
  }

}
