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

package com.baremaps.testing;

import java.nio.file.Path;
import org.locationtech.jts.geom.*;

public class TestFiles {

  private TestFiles() {
    // Prevent instantiation
  }

  /* The paths of the sample directory and files */

  public static final Path SAMPLE_DIR =
      resolve("baremaps-testing/data/osm-sample/");

  public static final Path SAMPLE_STATE_TXT =
      resolve("baremaps-testing/data/osm-sample/state.txt");

  public static final Path SAMPLE_OSM_XML =
      resolve("baremaps-testing/data/osm-sample/sample.osm.xml");

  public static final Path SAMPLE_OSM_PBF =
      resolve("baremaps-testing/data/osm-sample/sample.osm.pbf");

  public static final Path SAMPLE_OSC_XML_2 =
      resolve("baremaps-testing/data/osm-sample/000/000/002.osc.gz");

  public static final Path SAMPLE_OSC_XML_3 =
      resolve("baremaps-testing/data/osm-sample/000/000/003.osc.gz");

  public static final Path SAMPLE_OSC_XML_4 =
      resolve("baremaps-testing/data/osm-sample/000/000/004.osc.gz");

  public static final Path CONFIG_STYLE_JS =
      resolve("baremaps-testing/data/config/style.js");

  public static final Path ARCHIVE_FILE_BZ2 =
      resolve("baremaps-testing/data/archives/file.bz2");

  public static final Path ARCHIVE_FILE_GZ =
      resolve("baremaps-testing/data/archives/file.gz");

  public static final Path ARCHIVE_FILE_TAR_BZ2 =
      resolve("baremaps-testing/data/archives/file.tar.bz2");

  public static final Path ARCHIVE_FILE_TAR_GZ =
      resolve("baremaps-testing/data/archives/file.tar.gz");

  public static final Path ARCHIVE_FILE_ZIP =
      resolve("baremaps-testing/data/archives/file.zip");

  public static final Path GEOPARQUET =
      resolve("baremaps-testing/data/geoparquet/example.parquet");

  public static final Path GEOPACKAGE =
      resolve("baremaps-testing/data/geopackage/countries.gpkg");

  public static final Path TILESET_JSON =
      resolve("baremaps-testing/data/tilesets/tileset.json");

  public static final Path TILEJSON_JSON =
      resolve("baremaps-testing/data/tilesets/tilejson.json");

  public static final Path GEONAMES_CSV =
      resolve("baremaps-testing/data/geonames/sample.txt");

  public static final Path SAMPLE_CSV_DIR =
      resolve("baremaps-testing/data/csv/");

  public static final Path CITIES_CSV =
      resolve("baremaps-testing/data/csv/cities.csv");

  public static final Path COUNTRIES_CSV =
      resolve("baremaps-testing/data/csv/countries.csv");

  public static final Path POINT_SHP =
      resolve("baremaps-testing/data/shapefiles/point.shp");


  public static final Path SAMPLE_FLATGEOBUF_DIR =
      resolve("baremaps-testing/data/flatgeobuf/");

  public static final Path POINT_FLATGEOBUF =
      resolve("baremaps-testing/data/flatgeobuf/countries.fgb");

  public static final Path RPSL_TXT =
      resolve("baremaps-testing/data/rpsl/sample.txt");

  /* The geometries of the osm-sample/sample.osm.xml file */

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  // Node (point)
  public static final Point NODE_POINT_1 = GEOMETRY_FACTORY.createPoint(new Coordinate(0.0, 0.0));

  // Way (line)
  public static final LineString WAY_LINESTRING_4 =
      GEOMETRY_FACTORY.createLineString(new Coordinate[] {
          new Coordinate(1.0, 1.0),
          new Coordinate(2.0, 2.0)
      });

  // Way (polygon)
  public static final Polygon WAY_POLYGON_9 = GEOMETRY_FACTORY.createPolygon(new Coordinate[] {
      new Coordinate(2.0, 2.0),
      new Coordinate(2.0, 3.0),
      new Coordinate(3.0, 3.0),
      new Coordinate(3.0, 2.0),
      new Coordinate(2.0, 2.0)
  });

  // Relation (polygon with hole)
  public static final MultiPolygon RELATION_MULTIPOLYGON_20 = GEOMETRY_FACTORY.createMultiPolygon(
      new Polygon[] {
          GEOMETRY_FACTORY.createPolygon(
              GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                  new Coordinate(3.0, 3.0),
                  new Coordinate(3.0, 4.0),
                  new Coordinate(4.0, 4.0),
                  new Coordinate(4.0, 3.0),
                  new Coordinate(3.0, 3.0)
              }),
              new LinearRing[] {
                  GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                      new Coordinate(3.4, 3.4),
                      new Coordinate(3.6, 3.4),
                      new Coordinate(3.6, 3.6),
                      new Coordinate(3.4, 3.6),
                      new Coordinate(3.4, 3.4)
                  })
              })
      });

  // Relation (polygon with island and hole)
  public static final MultiPolygon RELATION_MULTIPOLYGON_36 =
      GEOMETRY_FACTORY.createMultiPolygon(new Polygon[] {
          GEOMETRY_FACTORY.createPolygon(
              GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                  new Coordinate(4.0, 4.0),
                  new Coordinate(4.0, 4.6),
                  new Coordinate(4.6, 4.6),
                  new Coordinate(4.6, 4.0),
                  new Coordinate(4.0, 4.0)
              }),
              new LinearRing[] {
                  GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                      new Coordinate(4.2, 4.2),
                      new Coordinate(4.4, 4.2),
                      new Coordinate(4.4, 4.4),
                      new Coordinate(4.2, 4.4),
                      new Coordinate(4.2, 4.2)
                  })
              }),
          GEOMETRY_FACTORY.createPolygon(
              GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                  new Coordinate(4.8, 4.8),
                  new Coordinate(4.8, 5.0),
                  new Coordinate(5.0, 5.0),
                  new Coordinate(5.0, 4.8),
                  new Coordinate(4.8, 4.8)
              }))
      });

  /* The geometries of the osm-sample/000/000/002.osc.gz file */

  // Node (point)
  public static final Point NODE_POINT_37 = GEOMETRY_FACTORY.createPoint(new Coordinate(6.0, 6.0));

  // Way (line)
  public static final LineString WAY_LINESTRING_40 =
      GEOMETRY_FACTORY.createLineString(new Coordinate[] {
          new Coordinate(7.0, 7.0),
          new Coordinate(8.0, 8.0)
      });

  // Way (polygon)
  public static final Polygon WAY_POLYGON_45 = GEOMETRY_FACTORY.createPolygon(new Coordinate[] {
      new Coordinate(8.0, 8.0),
      new Coordinate(8.0, 9.0),
      new Coordinate(9.0, 9.0),
      new Coordinate(9.0, 8.0),
      new Coordinate(8.0, 8.0)
  });

  // Relation (polygon with hole)
  public static final MultiPolygon RELATION_MULTIPOLYGON_56 = GEOMETRY_FACTORY.createMultiPolygon(
      new Polygon[] {
          GEOMETRY_FACTORY.createPolygon(
              GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                  new Coordinate(9.0, 9.0),
                  new Coordinate(9.0, 10.0),
                  new Coordinate(10.0, 10.0),
                  new Coordinate(10.0, 9.0),
                  new Coordinate(9.0, 9.0)
              }),
              new LinearRing[] {
                  GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                      new Coordinate(9.4, 9.4),
                      new Coordinate(9.6, 9.4),
                      new Coordinate(9.6, 9.6),
                      new Coordinate(9.4, 9.6),
                      new Coordinate(9.4, 9.4)
                  })
              })
      });

  // Relation (polygon with island and hole)
  public static final MultiPolygon RELATION_MULTIPOLYGON_72 =
      GEOMETRY_FACTORY.createMultiPolygon(new Polygon[] {
          GEOMETRY_FACTORY.createPolygon(
              GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                  new Coordinate(10.0, 10.0),
                  new Coordinate(10.0, 10.6),
                  new Coordinate(10.6, 10.6),
                  new Coordinate(10.6, 10.0),
                  new Coordinate(10.0, 10.0)
              }),
              new LinearRing[] {
                  GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                      new Coordinate(10.2, 10.2),
                      new Coordinate(10.4, 10.2),
                      new Coordinate(10.4, 10.4),
                      new Coordinate(10.2, 10.4),
                      new Coordinate(10.2, 10.2)
                  })
              }),
          GEOMETRY_FACTORY.createPolygon(
              GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                  new Coordinate(10.8, 10.8),
                  new Coordinate(10.8, 11.0),
                  new Coordinate(11.0, 11.0),
                  new Coordinate(11.0, 10.8),
                  new Coordinate(10.8, 10.8)
              }))
      });

  /* The geometries of the osm-sample/000/000/003.osc.gz file */

  // Node (point)
  public static final Point NODE_POINT_1_MODIFIED =
      GEOMETRY_FACTORY.createPoint(new Coordinate(0.5, 0.5));

  // Way (line)
  public static final LineString WAY_LINESTRING_4_MODIFIED =
      GEOMETRY_FACTORY.createLineString(new Coordinate[] {
          new Coordinate(1.2, 1.2),
          new Coordinate(1.8, 1.8)
      });

  // Way (polygon)
  public static final Polygon WAY_POLYGON_9_MODIFIED =
      GEOMETRY_FACTORY.createPolygon(new Coordinate[] {
          new Coordinate(2.1, 2.1),
          new Coordinate(2.1, 2.9),
          new Coordinate(2.9, 2.9),
          new Coordinate(2.9, 2.1),
          new Coordinate(2.1, 2.1)
      });

  // Relation (polygon with hole)
  public static final MultiPolygon RELATION_MULTIPOLYGON_20_MODIFIED =
      GEOMETRY_FACTORY.createMultiPolygon(
          new Polygon[] {
              GEOMETRY_FACTORY.createPolygon(
                  GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                      new Coordinate(3.0, 3.0),
                      new Coordinate(3.0, 4.0),
                      new Coordinate(4.0, 4.0),
                      new Coordinate(4.0, 3.0),
                      new Coordinate(3.0, 3.0)
                  }),
                  new LinearRing[] {
                      GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                          new Coordinate(3.5, 3.5),
                          new Coordinate(3.6, 3.5),
                          new Coordinate(3.6, 3.6),
                          new Coordinate(3.5, 3.6),
                          new Coordinate(3.5, 3.5)
                      })
                  })
          });

  public static final MultiPolygon RELATION_MULTIPOLYGON_36_MODIFIED =
      GEOMETRY_FACTORY.createMultiPolygon(new Polygon[] {
          GEOMETRY_FACTORY.createPolygon(
              GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                  new Coordinate(4.0, 4.0),
                  new Coordinate(4.0, 4.6),
                  new Coordinate(4.6, 4.6),
                  new Coordinate(4.6, 4.0),
                  new Coordinate(4.0, 4.0)
              }),
              new LinearRing[] {
                  GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                      new Coordinate(4.2, 4.2),
                      new Coordinate(4.4, 4.2),
                      new Coordinate(4.4, 4.4),
                      new Coordinate(4.2, 4.4),
                      new Coordinate(4.2, 4.2)
                  })
              }),
          GEOMETRY_FACTORY.createPolygon(
              GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                  new Coordinate(4.9, 4.9),
                  new Coordinate(4.9, 5.0),
                  new Coordinate(5.0, 5.0),
                  new Coordinate(5.0, 4.9),
                  new Coordinate(4.9, 4.9)
              }))
      });

  public static Path resolve(String resource) {
    Path cwd = Path.of("").toAbsolutePath();
    return cwd.resolveSibling(resource).toAbsolutePath();
  }

}
