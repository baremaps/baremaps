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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.locationtech.jts.geom.PrecisionModel;

/**
 * A Calcite table implementation for GeoPackage data. This table reads data from a GeoPackage file
 * and makes it available through the Apache Calcite framework for SQL querying.
 */
public class GeoPackageTable extends AbstractTable implements ScannableTable {

  private final File file;
  private final String tableName;
  private final RelDataType rowType;
  private final FeatureDao featureDao;
  private final GeometryFactory geometryFactory;

  /**
   * Constructs a GeoPackageTable with the specified file and table name.
   *
   * @param file the GeoPackage file to read data from
   * @param tableName the name of the table in the GeoPackage
   * @throws IOException if an I/O error occurs
   */
  public GeoPackageTable(File file, String tableName) throws IOException {
    this(file, tableName, new org.apache.calcite.jdbc.JavaTypeFactoryImpl());
  }

  /**
   * Constructs a GeoPackageTable with the specified file, table name, and type factory.
   *
   * @param file the GeoPackage file to read data from
   * @param tableName the name of the table in the GeoPackage
   * @param typeFactory the type factory
   * @throws IOException if an I/O error occurs
   */
  public GeoPackageTable(File file, String tableName, RelDataTypeFactory typeFactory)
      throws IOException {
    this.file = file;
    this.tableName = tableName;

    // Open the GeoPackage file
    GeoPackage geoPackage = GeoPackageManager.open(file);
    this.featureDao = geoPackage.getFeatureDao(tableName);

    // Create a geometry factory with the SRS from the feature DAO
    this.geometryFactory = new GeometryFactory(
        new PrecisionModel(),
        (int) featureDao.getSrs().getId());

    // Create the row type based on the feature columns
    this.rowType = createRowType(typeFactory);
  }

  /**
   * Creates a row type based on the feature columns.
   *
   * @param typeFactory the type factory
   * @return the row type
   */
  private RelDataType createRowType(RelDataTypeFactory typeFactory) {
    List<RelDataType> types = new ArrayList<>();
    List<String> names = new ArrayList<>();

    for (FeatureColumn column : featureDao.getColumns()) {
      String columnName = column.getName();
      RelDataType sqlType;

      if (column.isGeometry()) {
        // For geometry columns, use a proper geometry type
        sqlType = typeFactory.createJavaType(org.locationtech.jts.geom.Geometry.class);
      } else {
        // Map Java types to SQL types
        Class<?> javaType = column.getDataType().getClassType();
        if (javaType == String.class) {
          sqlType = typeFactory.createSqlType(SqlTypeName.VARCHAR);
        } else if (javaType == Integer.class || javaType == int.class) {
          sqlType = typeFactory.createSqlType(SqlTypeName.INTEGER);
        } else if (javaType == Long.class || javaType == long.class) {
          sqlType = typeFactory.createSqlType(SqlTypeName.BIGINT);
        } else if (javaType == Double.class || javaType == double.class) {
          sqlType = typeFactory.createSqlType(SqlTypeName.DOUBLE);
        } else if (javaType == Float.class || javaType == float.class) {
          sqlType = typeFactory.createSqlType(SqlTypeName.FLOAT);
        } else if (javaType == Boolean.class || javaType == boolean.class) {
          sqlType = typeFactory.createSqlType(SqlTypeName.BOOLEAN);
        } else if (javaType == Date.class) {
          sqlType = typeFactory.createSqlType(SqlTypeName.TIMESTAMP);
        } else {
          // Default to VARCHAR for unknown types
          sqlType = typeFactory.createSqlType(SqlTypeName.VARCHAR);
        }
      }

      // Handle nullability
      if (!column.isNotNull()) {
        sqlType = typeFactory.createTypeWithNullability(sqlType, true);
      }

      types.add(sqlType);
      names.add(columnName);
    }

    return typeFactory.createStructType(types, names);
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
        try {
          return new GeoPackageEnumerator(featureDao.queryForAll());
        } catch (Exception e) {
          throw new RuntimeException("Failed to create GeoPackage enumerator", e);
        }
      }
    };
  }

  /**
   * Enumerator for GeoPackage data.
   */
  private class GeoPackageEnumerator implements Enumerator<Object[]> {

    private final FeatureResultSet featureResultSet;
    private boolean hasNext;

    public GeoPackageEnumerator(FeatureResultSet featureResultSet) {
      this.featureResultSet = featureResultSet;
      this.hasNext = featureResultSet.moveToFirst();
    }

    @Override
    public Object[] current() {
      if (!hasNext) {
        return new Object[0];
      }

      Object[] values = new Object[featureResultSet.getColumns().getColumns().size()];
      int i = 0;

      for (FeatureColumn column : featureResultSet.getColumns().getColumns()) {
        Object value = featureResultSet.getValue(column);
        values[i++] = convertValue(value);
      }

      return values;
    }

    @Override
    public boolean moveNext() {
      if (!hasNext) {
        return false;
      }

      hasNext = featureResultSet.moveToNext();
      return hasNext;
    }

    @Override
    public void reset() {
      featureResultSet.moveToFirst();
      hasNext = true;
    }

    @Override
    public void close() {
      featureResultSet.close();
    }
  }

  /**
   * Converts a GeoPackage value to a Java value.
   *
   * @param value the GeoPackage value
   * @return the Java value
   */
  private Object convertValue(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof GeoPackageGeometryData) {
      GeoPackageGeometryData geometryData = (GeoPackageGeometryData) value;
      return convertGeometry(geometryData.getGeometry());
    }

    return value;
  }

  /**
   * Converts a GeoPackage geometry to a JTS geometry.
   *
   * @param geometry the GeoPackage geometry
   * @return the JTS geometry
   */
  private org.locationtech.jts.geom.Geometry convertGeometry(Geometry geometry) {
    if (geometry == null) {
      return null;
    }

    if (geometry instanceof Point point) {
      return convertPoint(point);
    } else if (geometry instanceof LineString lineString) {
      return convertLineString(lineString);
    } else if (geometry instanceof Polygon polygon) {
      return convertPolygon(polygon);
    } else if (geometry instanceof MultiPoint multiPoint) {
      return convertMultiPoint(multiPoint);
    } else if (geometry instanceof MultiLineString multiLineString) {
      return convertMultiLineString(multiLineString);
    } else if (geometry instanceof MultiPolygon multiPolygon) {
      return convertMultiPolygon(multiPolygon);
    } else if (geometry instanceof GeometryCollection<? extends Geometry>geometryCollection) {
      return convertGeometryCollection(geometryCollection);
    } else {
      // Unknown geometries are discarded
      return null;
    }
  }

  /**
   * Converts a GeoPackage geometry collection to a JTS geometry collection.
   *
   * @param geometryCollection the GeoPackage geometry collection
   * @return the JTS geometry collection
   */
  private org.locationtech.jts.geom.Geometry convertGeometryCollection(
      GeometryCollection<? extends Geometry> geometryCollection) {
    List<? extends Geometry> geometries = geometryCollection.getGeometries();
    org.locationtech.jts.geom.Geometry[] jtsGeometries = geometries.stream()
        .map(this::convertGeometry)
        .toArray(org.locationtech.jts.geom.Geometry[]::new);
    return geometryFactory.createGeometryCollection(jtsGeometries);
  }

  /**
   * Converts a GeoPackage multi polygon to a JTS multipolygon.
   *
   * @param multiPolygon the GeoPackage multipolygon
   * @return the JTS multipolygon
   */
  private org.locationtech.jts.geom.Geometry convertMultiPolygon(MultiPolygon multiPolygon) {
    org.locationtech.jts.geom.Polygon[] polygons = multiPolygon.getPolygons().stream()
        .map(this::convertPolygon)
        .toArray(org.locationtech.jts.geom.Polygon[]::new);
    return geometryFactory.createMultiPolygon(polygons);
  }

  /**
   * Converts a GeoPackage multilinestring to a JTS multi line string.
   *
   * @param multiLineString the GeoPackage multi line string
   * @return the JTS multi line string
   */
  private org.locationtech.jts.geom.Geometry convertMultiLineString(
      MultiLineString multiLineString) {
    org.locationtech.jts.geom.LineString[] lineStrings = multiLineString.getLineStrings().stream()
        .map(this::convertLineString)
        .toArray(org.locationtech.jts.geom.LineString[]::new);
    return geometryFactory.createMultiLineString(lineStrings);
  }

  /**
   * Converts a GeoPackage multipoint to a JTS multipoint.
   * 
   * @param multiPoint the GeoPackage multipoint
   * @return the JTS multipoint
   */
  private org.locationtech.jts.geom.Geometry convertMultiPoint(MultiPoint multiPoint) {
    org.locationtech.jts.geom.Point[] points = multiPoint.getPoints().stream()
        .map(this::convertPoint)
        .toArray(org.locationtech.jts.geom.Point[]::new);
    return geometryFactory.createMultiPoint(points);
  }

  /**
   * Converts a GeoPackage polygon to a JTS polygon.
   * 
   * @param polygon the GeoPackage polygon
   * @return the JTS polygon
   */
  private org.locationtech.jts.geom.Polygon convertPolygon(Polygon polygon) {
    org.locationtech.jts.geom.LinearRing shell = geometryFactory.createLinearRing(
        polygon.getExteriorRing().getPoints().stream()
            .map(point -> new Coordinate(point.getX(), point.getY()))
            .toArray(Coordinate[]::new));

    org.locationtech.jts.geom.LinearRing[] holes = polygon.getRings().stream()
        .skip(1)
        .map(lineString -> geometryFactory.createLinearRing(
            lineString.getPoints().stream()
                .map(point -> new Coordinate(point.getX(), point.getY()))
                .toArray(Coordinate[]::new)))
        .toArray(org.locationtech.jts.geom.LinearRing[]::new);

    return geometryFactory.createPolygon(shell, holes);
  }

  /**
   * Converts a GeoPackage linestring to a JTS linestring.
   *
   * @param lineString the GeoPackage linestring
   * @return the JTS linestring
   */
  private org.locationtech.jts.geom.LineString convertLineString(LineString lineString) {
    Coordinate[] coordinates = lineString.getPoints().stream()
        .map(point -> new Coordinate(point.getX(), point.getY()))
        .toArray(Coordinate[]::new);
    return geometryFactory.createLineString(coordinates);
  }

  /**
   * Converts a GeoPackage point to a JTS point.
   *
   * @param point the GeoPackage point
   * @return the JTS point
   */
  private org.locationtech.jts.geom.Geometry convertPoint(Point point) {
    return geometryFactory.createPoint(new Coordinate(point.getX(), point.getY()));
  }
}
