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

package com.baremaps.workflow;



import com.baremaps.tasks.*;
import com.baremaps.tasks.DecompressFile.Compression;
import com.baremaps.testing.PostgresContainerTest;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S2699")
class WorkflowTest extends PostgresContainerTest {

  @Test
  @Disabled("requires internet access")
  void naturalearthGeoPackage() {
    var workflow = new Workflow(List.of(new Step("fetch-geopackage", List.of(), List.of(
        new DownloadUrl("https://naciscdn.org/naturalearth/packages/natural_earth_vector.gpkg.zip",
            Paths.get("natural_earth_vector.gpkg.zip"), false),
        new DecompressFile(Paths.get("natural_earth_vector.gpkg.zip"),
            Paths.get("natural_earth_vector"), Compression.ZIP),
        new ImportGeoPackage(Paths.get("natural_earth_vector/packages/natural_earth_vector.gpkg"),
            4326, jdbcUrl(),
            3857)))));
    new WorkflowExecutor(workflow).execute();
  }

  @Test
  @Disabled("requires internet access")
  void coastlineShapefile() {
    var workflow = new Workflow(List.of(new Step("fetch-geopackage", List.of(),
        List.of(
            new DownloadUrl("https://osmdata.openstreetmap.de/download/coastlines-split-4326.zip",
                Paths.get("coastlines-split-4326.zip"), false),
            new DecompressFile(Paths.get("coastlines-split-4326.zip"),
                Paths.get("coastlines-split-4326"), Compression.ZIP),
            new ImportShapefile(Paths.get("coastlines-split-4326/coastlines-split-4326/lines.shp"),
                4326, jdbcUrl(),
                3857)))));
    new WorkflowExecutor(workflow).execute();
  }

  @Test
  @Disabled("requires internet access")
  void simplifiedWaterPolygonsShapefile() {
    var workflow = new Workflow(List.of(new Step("simplified-water-polygons", List.of(), List.of(
        /*
         * new DownloadUrl(
         * "https://osmdata.openstreetmap.de/download/simplified-water-polygons-split-3857.zip",
         * "simplified-water-polygons-split-3857.zip"), new UnzipFile(
         * "simplified-water-polygons-split-3857.zip", "simplified-water-polygons-split-3857"),
         */
        new ImportShapefile(
            Paths.get(
                "simplified-water-polygons-split-3857/simplified-water-polygons-split-3857/simplified_water_polygons.shp"),
            3857, "jdbc:postgresql://localhost:5432/baremaps?&user=baremaps&password=baremaps",
            3857)))));
    new WorkflowExecutor(workflow).execute();
  }

  @Test
  @Disabled("requires internet access")
  void workflow() {
    var workflow = new Workflow(List.of(new Step("fetch-geopackage", List.of(), List.of(
        new DownloadUrl("https://naciscdn.org/naturalearth/packages/natural_earth_vector.gpkg.zip",
            Paths.get("downloads/import_db.gpkg"), false),
        new ImportShapefile(Paths.get("downloads/import_db.gpkg"), 4326, jdbcUrl(), 3857)))));
    new WorkflowExecutor(workflow).execute();
  }

  @Test
  @Disabled("requires internet access")
  void execute() {
    var workflow = new Workflow(List.of(
        new Step("fetch-geopackage", List.of(),
            List.of(new DownloadUrl("https://tiles.baremaps.com/samples/import_db.gpkg",
                Paths.get("downloads/import_db.gpkg"), false))),
        new Step("import-geopackage", List.of("fetch-geopackage"),
            List.of(new ImportGeoPackage(Paths.get("downloads/import_db.gpkg"), 4326, jdbcUrl(),
                3857))),
        new Step("fetch-osmpbf", List.of(),
            List.of(new DownloadUrl("https://tiles.baremaps.com/samples/liechtenstein.osm.pbf",
                Paths.get("downloads/liechtenstein.osm.pbf"), false))),
        new Step("import-osmpbf", List.of("fetch-osmpbf"),
            List.of(new ImportOsmPbf(Paths.get("downloads/liechtenstein.osm.pbf"),
                jdbcUrl(),
                3857, true, true))),
        new Step("fetch-shapefile", List.of(), List.of(new DownloadUrl(
            "https://osmdata.openstreetmap.de/download/simplified-water-polygons-split-3857.zip",
            Paths.get("downloads/simplified-water-polygons-split-3857.zip"), false))),
        new Step("unzip-shapefile", List.of("fetch-shapefile"),
            List.of(
                new DecompressFile(Paths.get("downloads/simplified-water-polygons-split-3857.zip"),
                    Paths.get("archives"), Compression.ZIP))),
        new Step("fetch-projection", List.of("unzip-shapefile"),
            List.of(new DownloadUrl("https://spatialreference.org/ref/sr-org/epsg3857/prj/",
                Paths.get(
                    "archives/simplified-water-polygons-split-3857/simplified_water_polygons.prj"),
                false))),
        new Step("import-shapefile", List.of("fetch-projection"),
            List.of(new ImportShapefile(
                Paths.get(
                    "archives/simplified-water-polygons-split-3857/simplified_water_polygons.shp"),
                3857, jdbcUrl(), 3857)))));
    new WorkflowExecutor(workflow).execute();
  }
}
