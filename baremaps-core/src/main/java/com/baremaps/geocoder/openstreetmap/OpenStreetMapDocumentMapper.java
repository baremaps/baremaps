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

package com.baremaps.geocoder.openstreetmap;



import com.baremaps.openstreetmap.model.Element;
import com.baremaps.openstreetmap.model.Node;
import java.util.function.Function;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LatLonShape;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.geo.Polygon;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OpenStreetMapDocumentMapper implements Function<Element, Document> {
  private static final Logger logger = LoggerFactory.getLogger(OpenStreetMapDocumentMapper.class);

  @Override
  public Document apply(Element element) {
    var document = new Document();
    document.add(new StoredField("osm_id", element.getId()));
    document.add(new StoredField("osm_type", element.getClass().getSimpleName()));

    if (element.getTags().containsKey(OsmTags.NAME.key())) {
      document.add(
          new TextField(OsmTags.NAME.key(), element.getTags().get(OsmTags.NAME.key()).toString(),
              Field.Store.YES));
    }

    if (element instanceof Node node) {
      document.add(LatLonShape.createIndexableFields("polygon", node.getLat(), node.getLon())[0]);
      document.add(new StoredField("latitude", node.getLat()));
      document.add(new StoredField("longitude", node.getLon()));
    }
    if (element.getGeometry() != null
        && element.getGeometry().getGeometryType().equals(Geometry.TYPENAME_LINESTRING)) {
      logger.debug("Geometry linestring ignored as not supported by Lucene Polygon.fromGeoJson: {}",
          element);
    }
    if (element.getGeometry() != null
        && !element.getGeometry().getGeometryType().equals(Geometry.TYPENAME_POINT)
        && !element.getGeometry().getGeometryType().equals(Geometry.TYPENAME_LINESTRING)) {
      // JTS to GeoJSON
      var geojsonWriter = new GeoJsonWriter();
      // Remove crs field in GeoJSON as the field content is incompatible between
      // Lucene Polygon.fromGeoJSON and GeoJsonWriter.
      // Avoid "crs must be CRS84 from OGC, but saw: EPSG:4326"
      // See:
      // https://github.com/apache/lucene/blob/ef42af65f27f7f078b1ab426de9f2b2fa214ad86/lucene/core/src/java/org/apache/lucene/geo/SimpleGeoJSONPolygonParser.java#L180
      // https://github.com/locationtech/jts/blob/ee59b591f15b5150516393d3ba0b49e46a113fc9/modules/io/common/src/main/java/org/locationtech/jts/io/geojson/GeoJsonWriter.java#L226
      geojsonWriter.setEncodeCRS(false);
      // Assume that Geometry is in EPSG:4326/WGS84 for Lucene Polygon.fromGeoJSON
      var geojson = geojsonWriter.write(element.getGeometry());

      // GeoJSON to Lucene Polygon
      try {
        var polygons = Polygon.fromGeoJSON(geojson);

        for (Polygon polygon : polygons) {
          // LatLonShape.createIndexableFields can create multiple polygons out of a single polygon
          // through tesselation
          for (Field field : LatLonShape.createIndexableFields("polygon", polygon)) {
            document.add(field);
          }
        }
      } catch (Exception e) {
        // ignore geometry
        logger.debug("Geometry ({}) failed indexing caused by: {}",
            element, e);
      }

    }

    if (element.getTags().containsKey(OsmTags.POPULATION.key())) {
      var population = Long.parseLong(element.getTags().get(OsmTags.POPULATION.key()).toString());
      document.add(new NumericDocValuesField(OsmTags.POPULATION.key(), population));
      document.add(new StoredField(OsmTags.POPULATION.key(), population));
    }
    return document;
  }

}
