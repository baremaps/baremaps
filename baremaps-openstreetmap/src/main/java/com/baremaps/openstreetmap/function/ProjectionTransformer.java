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

package com.baremaps.openstreetmap.function;



import com.baremaps.openstreetmap.utils.CRSUtils;
import com.baremaps.openstreetmap.utils.GeometryUtils;
import java.util.Objects;
import java.util.stream.Stream;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.geom.util.GeometryTransformer;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.ProjCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A transformer that reprojects geometries and sets the correct output SRIDs. */
public class ProjectionTransformer extends GeometryTransformer {

  private static final Logger logger = LoggerFactory.getLogger(ProjectionTransformer.class);

  private final int sourceSrid;

  private final int targetSrid;

  private final CoordinateTransform transform;

  private final ProjCoordinate min;

  private final ProjCoordinate max;

  /**
   * Creates a transformer that reprojects geometries with the provided SRIDs.
   *
   * @param sourceSrid the source SRID
   * @param targetSrid the target SRID
   */
  public ProjectionTransformer(int sourceSrid, int targetSrid) {
    this.sourceSrid = sourceSrid;
    this.targetSrid = targetSrid;
    this.transform = GeometryUtils.coordinateTransform(sourceSrid, targetSrid);

    var targetCRS = CRSUtils.createFromSrid(targetSrid);
    var lonlatTranform = GeometryUtils.coordinateTransform(4326, sourceSrid);
    min = lonlatTranform
        .transform(new ProjCoordinate(Math.toDegrees(targetCRS.getProjection().getMinLongitude()),
            Math.toDegrees(targetCRS.getProjection().getMinLatitude())), new ProjCoordinate());
    max = lonlatTranform
        .transform(new ProjCoordinate(Math.toDegrees(targetCRS.getProjection().getMaxLongitude()),
            Math.toDegrees(targetCRS.getProjection().getMaxLatitude())), new ProjCoordinate());
  }

  private Coordinate transformCoordinate(Coordinate coordinate) {
    if (sourceSrid == targetSrid) {
      return coordinate;
    }
    // Clamp to the domain of the target projection: e.g. Mercator is undefined at the poles, and
    // OSM data does contain nodes there.
    var x = Math.max(Math.min(coordinate.x, max.x), min.x);
    var y = Math.max(Math.min(coordinate.y, max.y), min.y);
    ProjCoordinate c1 = new ProjCoordinate(x, y);
    ProjCoordinate c2 = transform.transform(c1, new ProjCoordinate());
    return new Coordinate(c2.x, c2.y);
  }

  @Override
  protected CoordinateSequence transformCoordinates(
      CoordinateSequence coordinateSequence,
      Geometry parent) {
    Coordinate[] coordinateArray = Stream.of(coordinateSequence.toCoordinateArray())
        .filter(Objects::nonNull)
        .map(this::transformCoordinate).toArray(Coordinate[]::new);
    return new CoordinateArraySequence(coordinateArray);
  }

  @Override
  protected Geometry transformPoint(Point geom, Geometry parent) {
    try {
      var geometry = super.transformPoint(geom, parent);
      geometry.setSRID(targetSrid);
      return geometry;
    } catch (Exception e) {
      logger.error("Point cannot be reprojected", e);
      var geometry = parent.getFactory().createPoint();
      geometry.setSRID(targetSrid);
      return geometry;
    }
  }

  @Override
  protected Geometry transformMultiPoint(MultiPoint geom, Geometry parent) {
    try {
      var geometry = super.transformMultiPoint(geom, parent);
      if (geometry instanceof Point point) {
        geometry = factory.createMultiPoint(new Point[] {point});
      }
      geometry.setSRID(targetSrid);
      return geometry;
    } catch (Exception e) {
      logger.error("MultiPoint cannot be reprojected", e);
      var geometry = parent.getFactory().createMultiPoint();
      geometry.setSRID(targetSrid);
      return geometry;
    }
  }

  @Override
  protected Geometry transformLinearRing(LinearRing geom, Geometry parent) {
    try {
      var geometry = super.transformLinearRing(geom, parent);
      geometry.setSRID(targetSrid);
      return geometry;
    } catch (Exception e) {
      logger.error("LinearRing cannot be reprojected", e);
      var geometry = parent.getFactory().createLinearRing();
      geometry.setSRID(targetSrid);
      return geometry;
    }
  }

  @Override
  protected Geometry transformLineString(LineString geom, Geometry parent) {
    try {
      var geometry = super.transformLineString(geom, parent);
      geometry.setSRID(targetSrid);
      return geometry;
    } catch (Exception e) {
      logger.error("LineString cannot be reprojected", e);
      var geometry = parent.getFactory().createLineString();
      geometry.setSRID(targetSrid);
      return geometry;
    }
  }

  @Override
  protected Geometry transformMultiLineString(MultiLineString geom, Geometry parent) {
    try {
      var geometry = super.transformMultiLineString(geom, parent);
      if (geometry instanceof LineString lineString) {
        geometry = factory.createMultiLineString(new LineString[] {lineString});
      }
      geometry.setSRID(targetSrid);
      return geometry;
    } catch (Exception e) {
      logger.error("MultiLineString cannot be reprojected", e);
      var geometry = parent.getFactory().createMultiLineString();
      geometry.setSRID(targetSrid);
      return geometry;
    }
  }

  @Override
  protected Geometry transformPolygon(Polygon geom, Geometry parent) {
    try {
      var geometry = super.transformPolygon(geom, parent);
      geometry.setSRID(targetSrid);
      return geometry;
    } catch (Exception e) {
      logger.error("Polygon cannot be reprojected", e);
      var geometry = parent.getFactory().createPolygon();
      geometry.setSRID(targetSrid);
      return geometry;
    }
  }

  @Override
  protected Geometry transformMultiPolygon(MultiPolygon geom, Geometry parent) {
    try {
      var geometry = super.transformMultiPolygon(geom, parent);
      if (geometry instanceof Polygon polygon) {
        geometry = factory.createMultiPolygon(new Polygon[] {polygon});
      }
      geometry.setSRID(targetSrid);
      return geometry;
    } catch (Exception e) {
      logger.error("MultiPolygon cannot be reprojected", e);
      var geometry = parent.getFactory().createMultiPolygon();
      geometry.setSRID(targetSrid);
      return geometry;
    }
  }

  @Override
  protected Geometry transformGeometryCollection(GeometryCollection geom, Geometry parent) {
    try {
      var geometry = super.transformGeometryCollection(geom, parent);
      geometry.setSRID(targetSrid);
      return geometry;
    } catch (Exception e) {
      logger.error("GeometryCollection cannot be reprojected", e);
      var geometry = parent.getFactory().createGeometryCollection();
      geometry.setSRID(targetSrid);
      return geometry;
    }
  }
}
