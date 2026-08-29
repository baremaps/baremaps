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

package com.baremaps.calcite.geopackage;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.features.user.FeatureColumn;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureResultSet;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.sf.Geometry;
import mil.nga.sf.GeometryCollection;
import mil.nga.sf.LineString;
import mil.nga.sf.MultiLineString;
import mil.nga.sf.MultiPoint;
import mil.nga.sf.MultiPolygon;
import mil.nga.sf.Point;
import mil.nga.sf.Polygon;
import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.PrecisionModel;

/** A read-only table over a feature table of a GeoPackage file. */
public class GeoPackageTable extends AbstractTable implements ScannableTable {

  private final FeatureDao featureDao;
  private final GeometryFactory geometryFactory;
  private final RelDataType rowType;

  public GeoPackageTable(File file, String tableName, RelDataTypeFactory typeFactory)
      throws IOException {
    // The GeoPackage stays open for the lifetime of the table: the DAO needs its connection.
    GeoPackage geoPackage = GeoPackageManager.open(file);
    this.featureDao = geoPackage.getFeatureDao(tableName);
    this.geometryFactory =
        new GeometryFactory(new PrecisionModel(), (int) featureDao.getSrs().getId());
    this.rowType = rowType(typeFactory);
  }

  private RelDataType rowType(RelDataTypeFactory typeFactory) {
    RelDataTypeFactory.Builder builder = typeFactory.builder();
    for (FeatureColumn column : featureDao.getColumns()) {
      RelDataType type = typeFactory.createSqlType(sqlType(column));
      builder.add(column.getName(),
          typeFactory.createTypeWithNullability(type, !column.isNotNull()));
    }
    return builder.build();
  }

  private static SqlTypeName sqlType(FeatureColumn column) {
    if (column.isGeometry()) {
      return SqlTypeName.GEOMETRY;
    }
    Class<?> javaType = column.getDataType().getClassType();
    if (javaType == String.class) {
      return SqlTypeName.VARCHAR;
    } else if (javaType == Integer.class || javaType == int.class) {
      return SqlTypeName.INTEGER;
    } else if (javaType == Long.class || javaType == long.class) {
      return SqlTypeName.BIGINT;
    } else if (javaType == Double.class || javaType == double.class) {
      return SqlTypeName.DOUBLE;
    } else if (javaType == Float.class || javaType == float.class) {
      return SqlTypeName.FLOAT;
    } else if (javaType == Boolean.class || javaType == boolean.class) {
      return SqlTypeName.BOOLEAN;
    } else if (javaType == Date.class) {
      return SqlTypeName.TIMESTAMP;
    } else {
      return SqlTypeName.VARCHAR;
    }
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    return rowType;
  }

  @Override
  public Enumerable<Object[]> scan(DataContext root) {
    return new AbstractEnumerable<>() {
      @Override
      public Enumerator<Object[]> enumerator() {
        return new GeoPackageEnumerator(featureDao.queryForAll());
      }
    };
  }

  private class GeoPackageEnumerator implements Enumerator<Object[]> {

    private final FeatureResultSet resultSet;
    private boolean hasNext;

    GeoPackageEnumerator(FeatureResultSet resultSet) {
      this.resultSet = resultSet;
      this.hasNext = resultSet.moveToFirst();
    }

    @Override
    public Object[] current() {
      if (!hasNext) {
        return null;
      }
      Object[] values = new Object[resultSet.getColumns().getColumns().size()];
      int i = 0;
      for (FeatureColumn column : resultSet.getColumns().getColumns()) {
        Object value = resultSet.getValue(column);
        values[i++] = value instanceof GeoPackageGeometryData data
            ? toJts(data.getGeometry())
            : value;
      }
      return values;
    }

    @Override
    public boolean moveNext() {
      if (!hasNext) {
        return false;
      }
      hasNext = resultSet.moveToNext();
      return hasNext;
    }

    @Override
    public void reset() {
      hasNext = resultSet.moveToFirst();
    }

    @Override
    public void close() {
      resultSet.close();
    }
  }

  /** Converts a GeoPackage geometry to JTS; unsupported geometries become null. */
  private org.locationtech.jts.geom.Geometry toJts(Geometry geometry) {
    if (geometry instanceof Point point) {
      return toJts(point);
    } else if (geometry instanceof LineString lineString) {
      return toJts(lineString);
    } else if (geometry instanceof Polygon polygon) {
      return toJts(polygon);
    } else if (geometry instanceof MultiPoint multiPoint) {
      return geometryFactory.createMultiPoint(multiPoint.getPoints().stream()
          .map(this::toJts).toArray(org.locationtech.jts.geom.Point[]::new));
    } else if (geometry instanceof MultiLineString multiLineString) {
      return geometryFactory.createMultiLineString(multiLineString.getLineStrings().stream()
          .map(this::toJts).toArray(org.locationtech.jts.geom.LineString[]::new));
    } else if (geometry instanceof MultiPolygon multiPolygon) {
      return geometryFactory.createMultiPolygon(multiPolygon.getPolygons().stream()
          .map(this::toJts).toArray(org.locationtech.jts.geom.Polygon[]::new));
    } else if (geometry instanceof GeometryCollection<?>collection) {
      return geometryFactory.createGeometryCollection(collection.getGeometries().stream()
          .map(this::toJts).toArray(org.locationtech.jts.geom.Geometry[]::new));
    } else {
      return null;
    }
  }

  private org.locationtech.jts.geom.Point toJts(Point point) {
    return geometryFactory.createPoint(new Coordinate(point.getX(), point.getY()));
  }

  private org.locationtech.jts.geom.LineString toJts(LineString lineString) {
    return geometryFactory.createLineString(coordinates(lineString));
  }

  private org.locationtech.jts.geom.Polygon toJts(Polygon polygon) {
    LinearRing shell = geometryFactory.createLinearRing(coordinates(polygon.getExteriorRing()));
    LinearRing[] holes = polygon.getRings().stream()
        .skip(1)
        .map(ring -> geometryFactory.createLinearRing(coordinates(ring)))
        .toArray(LinearRing[]::new);
    return geometryFactory.createPolygon(shell, holes);
  }

  private static Coordinate[] coordinates(LineString lineString) {
    return lineString.getPoints().stream()
        .map(point -> new Coordinate(point.getX(), point.getY()))
        .toArray(Coordinate[]::new);
  }
}
