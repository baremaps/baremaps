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

package com.baremaps.data.geometry;

import java.util.function.Supplier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.geom.util.GeometryTransformer;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.ProjCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A transformer that reprojects geometries and stamps them with the target SRID.
 *
 * <p>
 * Like the {@link GeometryTransformer} it extends, an instance keeps the geometry being transformed
 * in a field and is therefore not re-entrant: it can be reused across calls, but not shared between
 * threads.
 */
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
    var lonLatTransform = GeometryUtils.coordinateTransform(GeometryUtils.WGS84, sourceSrid);
    this.min = lonLatTransform.transform(
        new ProjCoordinate(Math.toDegrees(targetCRS.getProjection().getMinLongitude()),
            Math.toDegrees(targetCRS.getProjection().getMinLatitude())),
        new ProjCoordinate());
    this.max = lonLatTransform.transform(
        new ProjCoordinate(Math.toDegrees(targetCRS.getProjection().getMaxLongitude()),
            Math.toDegrees(targetCRS.getProjection().getMaxLatitude())),
        new ProjCoordinate());
  }

  /**
   * Applies a transformation and stamps its result with the target SRID.
   *
   * <p>
   * A geometry that cannot be reprojected is replaced by an empty geometry of the same kind rather
   * than propagated as an error: a single malformed feature must not abort an import of millions.
   * Because every kind goes through this method, a failure inside a collection is contained to the
   * component that caused it.
   */
  private Geometry reproject(Geometry source, Supplier<Geometry> transformation) {
    Geometry result;
    try {
      result = transformation.get();
    } catch (Exception e) {
      logger.error("{} cannot be reprojected", source.getGeometryType(), e);
      result = createEmpty(source);
    }
    result.setSRID(targetSrid);
    return result;
  }

  @Override
  protected CoordinateSequence transformCoordinates(CoordinateSequence sequence, Geometry parent) {
    if (sourceSrid == targetSrid) {
      return sequence;
    }
    var coordinates = sequence.toCoordinateArray();
    var transformed = new Coordinate[coordinates.length];
    for (int i = 0; i < coordinates.length; i++) {
      // Clamp to the domain of the target projection: e.g. Mercator is undefined at the poles, and
      // OSM data does contain nodes there.
      var x = Math.max(Math.min(coordinates[i].x, max.x), min.x);
      var y = Math.max(Math.min(coordinates[i].y, max.y), min.y);
      var projected = transform.transform(new ProjCoordinate(x, y), new ProjCoordinate());
      transformed[i] = new Coordinate(projected.x, projected.y);
    }
    return new CoordinateArraySequence(transformed);
  }

  @Override
  protected Geometry transformPoint(Point geom, Geometry parent) {
    return reproject(geom, () -> super.transformPoint(geom, parent));
  }

  @Override
  protected Geometry transformLinearRing(LinearRing geom, Geometry parent) {
    return reproject(geom, () -> super.transformLinearRing(geom, parent));
  }

  @Override
  protected Geometry transformLineString(LineString geom, Geometry parent) {
    return reproject(geom, () -> super.transformLineString(geom, parent));
  }

  @Override
  protected Geometry transformPolygon(Polygon geom, Geometry parent) {
    return reproject(geom, () -> super.transformPolygon(geom, parent));
  }

  @Override
  protected Geometry transformGeometryCollection(GeometryCollection geom, Geometry parent) {
    return reproject(geom, () -> super.transformGeometryCollection(geom, parent));
  }

  // JTS collapses a transformed collection of one element to that element. The three overrides
  // below restore the declared type, so a MultiPolygon column never receives a bare Polygon.

  @Override
  protected Geometry transformMultiPoint(MultiPoint geom, Geometry parent) {
    return reproject(geom, () -> {
      var geometry = super.transformMultiPoint(geom, parent);
      return geometry instanceof Point point
          ? factory.createMultiPoint(new Point[] {point})
          : geometry;
    });
  }

  @Override
  protected Geometry transformMultiLineString(MultiLineString geom, Geometry parent) {
    return reproject(geom, () -> {
      var geometry = super.transformMultiLineString(geom, parent);
      return geometry instanceof LineString lineString
          ? factory.createMultiLineString(new LineString[] {lineString})
          : geometry;
    });
  }

  @Override
  protected Geometry transformMultiPolygon(MultiPolygon geom, Geometry parent) {
    return reproject(geom, () -> {
      var geometry = super.transformMultiPolygon(geom, parent);
      return geometry instanceof Polygon polygon
          ? factory.createMultiPolygon(new Polygon[] {polygon})
          : geometry;
    });
  }

  /** Returns an empty geometry of the same kind as the provided one. */
  private static Geometry createEmpty(Geometry geometry) {
    GeometryFactory geometryFactory = geometry.getFactory();
    return switch (geometry) {
      case Point ignored -> geometryFactory.createPoint();
      case MultiPoint ignored -> geometryFactory.createMultiPoint();
      // A LinearRing is a LineString, so it has to be matched first.
      case LinearRing ignored -> geometryFactory.createLinearRing();
      case LineString ignored -> geometryFactory.createLineString();
      case MultiLineString ignored -> geometryFactory.createMultiLineString();
      case Polygon ignored -> geometryFactory.createPolygon();
      case MultiPolygon ignored -> geometryFactory.createMultiPolygon();
      default -> geometryFactory.createGeometryCollection();
    };
  }
}
