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

package com.baremaps.openstreetmap;

import static com.baremaps.testing.TestFiles.resolve;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.openstreetmap.model.Element;
import com.baremaps.openstreetmap.model.Entity;
import com.baremaps.openstreetmap.pbf.PbfEntityReader;
import com.baremaps.openstreetmap.xml.XmlEntityReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.geom.util.GeometryTransformer;
import org.locationtech.jts.io.WKTReader;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;


class OsmDataTest {

  @TestFactory
  Stream<DynamicTest> runTests() throws IOException {
    var directory = resolve("baremaps-testing/data/osm-testdata");
    try (var files = Files.walk(directory)) {
      return files.filter(f -> f.endsWith("test.json"))
          .map(OsmTest::new)
          .filter(OsmTest::isValid)
          .sorted()
          .flatMap(this::createDynamicTest)
          .toList().stream();
    }
  }

  @NotNull
  Stream<DynamicTest> createDynamicTest(OsmTest osmTest) {
    String displayNameFormat = "%s (%s): %s";
    var xmlDisplayName =
        String.format(displayNameFormat, osmTest.getId(), "xml", osmTest.getDescription());
    var pbfDisplayName =
        String.format(displayNameFormat, osmTest.getId(), "pbf", osmTest.getDescription());
    return Stream.<DynamicTest>builder()
        .add(DynamicTest.dynamicTest(xmlDisplayName, () -> runTest(osmTest, new XmlEntityReader()
            .setCoordinateMap(new HashMap<>())
            .setReferenceMap(new HashMap<>())
            .setGeometries(true)
            .read(Files.newInputStream(osmTest.getOsmXml())))))
        .add(DynamicTest.dynamicTest(pbfDisplayName, () -> runTest(osmTest, new PbfEntityReader()
            .setCoordinateMap(new HashMap<>())
            .setReferenceMap(new HashMap<>())
            .setGeometries(true)
            .read(Files.newInputStream(osmTest.getOsmPbf())))))
        .build();
  }

  void runTest(OsmTest osmTest, Stream<Entity> entities) {
    var elements = entities
        .filter(e -> e instanceof Element)
        .map(e -> (Element) e)
        .toList();

    for (var element : elements) {
      // Each element should have a geometry
      var id = element.getId();
      var fileGeometry = element.getGeometry();
      Assert.assertNotNull(fileGeometry);

      // Prepare the test geometry
      var testWkt = osmTest.getWkts().get(id);
      Geometry testGeometry = null;
      try {
        testGeometry = new WKTReader().read(testWkt);
      } catch (Exception e) {
        // ignore
      }
      if (testGeometry instanceof LineString lineString
          && lineString.isClosed()) {
        testGeometry =
            testGeometry.getFactory().createPolygon(lineString.getCoordinateSequence());
      }
      if (testGeometry instanceof MultiPolygon multiPolygon
          && multiPolygon.getNumGeometries() == 1) {
        testGeometry = multiPolygon.getGeometryN(0);
      }
      if (!testGeometry.isValid()) {
        var geometryFixer = new GeometryFixer(testGeometry);
        testGeometry = geometryFixer.getResult();
      }

      // The test geometry and the file geometry should be equal
      var message = String.format("%s: %s%nExpected:%n%s%nActual:%n%s",
          osmTest.getId(), osmTest.getDescription(), testGeometry, fileGeometry);

      RoundingTransformer transformer = new RoundingTransformer(2);
      fileGeometry = transformer.transform(fileGeometry);

      assertTrue(testGeometry.equalsTopo(fileGeometry), message);
    }
  }

  static class OsmTest implements Comparable<OsmTest> {

    private static final ObjectMapper objectMapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Path jsonFile;

    private final JsonNode testJson;

    public OsmTest(Path jsonFile) {
      this.jsonFile = jsonFile;
      try {
        this.testJson = objectMapper.readValue(jsonFile.toFile(), JsonNode.class);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public Long getId() {
      return testJson.get("test_id").asLong();
    }

    public String getDescription() {
      return testJson.get("description").asText();
    }

    public Path getJsonFile() {
      return jsonFile;
    }

    public Path getDirectory() {
      return jsonFile.getParent();
    }

    public boolean isValid() {
      try {
        return Files.readString(getDirectory().resolve("result")).trim().equals("valid");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public JsonNode getTestJson() {
      return testJson;
    }

    public Map<Long, String> getWkts() {
      var wkts = new HashMap<Long, String>();
      wkts.putAll(getNodes());
      wkts.putAll(getWays());
      wkts.putAll(getRelations());
      wkts.putAll(getAreas());
      return wkts;
    }

    public Map<Long, String> getNodes() {
      var wkts = new HashMap<Long, String>();
      extractWktFile("nodes.wkt", wkts);
      return wkts;
    }

    public Map<Long, String> getWays() {
      var wkts = new HashMap<Long, String>();
      extractWktFile("ways.wkt", wkts);
      return wkts;
    }

    public Map<Long, String> getRelations() {
      var wkts = new HashMap<Long, String>();
      extractWktFile("relations.wkt", wkts);
      return wkts;
    }

    public Map<Long, String> getAreas() {
      var wkts = new HashMap<Long, String>();
      if (testJson.has("areas")) {
        var areas = testJson.get("areas");
        extractAreas(areas, "default", wkts);
        extractAreas(areas, "location", wkts);
        extractAreas(areas, "fixed", wkts);
        extractAreas(areas, "fix", wkts);
      }
      return wkts;
    }

    private void extractAreas(JsonNode areas, String name, Map<Long, String> wkts) {
      if (areas.has(name)) {
        var defaultAreas = areas.get(name);
        if (defaultAreas.isArray()) {
          for (var area : defaultAreas) {
            if (area.has("wkt")) {
              var wkt = area.get("wkt").asText();
              var id = area.get("from_id").asLong();
              wkts.put(id, wkt);
            }
          }
        }
      }
    }

    private void extractWktFile(String wktFile, Map<Long, String> wkts) {
      parseWkt(getDirectory().resolve(wktFile), wkts);
    }

    private void parseWkt(Path path, Map<Long, String> geometries) {
      if (!Files.exists(path)) {
        return;
      }
      try {
        Files.lines(path).forEach(line -> {
          var needle = line.indexOf(' ');
          var id = Long.parseLong(line.substring(0, needle).strip());
          var wkt = line.substring(needle + 1).strip();
          geometries.put(id, wkt);
        });
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public Path getOsmXml() {
      return getDirectory().resolve("data.osm");
    }

    public Path getOsmPbf() {
      return getDirectory().resolve("data.osm.pbf");
    }

    @Override
    public int compareTo(@NotNull OsmDataTest.OsmTest o) {
      return Long.compare(getId(), o.getId());
    }
  }

  /**
   * A transformer that rounds the coordinates of a geometry to a given precision.
   */
  static class RoundingTransformer extends GeometryTransformer {

    private final int precision;

    /**
     * Constructs a transformer that rounds the coordinates of a geometry to a given precision.
     *
     * @param precision the precision
     */
    public RoundingTransformer(int precision) {
      this.precision = precision;
    }

    /**
     * Rounds the coordinates of a geometry to a given precision.
     *
     * @param sequence the coordinate sequence
     * @param parent the parent geometry
     * @return the geometry
     */
    @Override
    protected CoordinateSequence transformCoordinates(CoordinateSequence sequence,
        Geometry parent) {
      CoordinateSequence rounded = super.transformCoordinates(sequence, parent);
      for (int i = 0; i < rounded.size(); i++) {
        double roundedX =
            Math.round(rounded.getOrdinate(i, CoordinateSequence.X) * Math.pow(10, precision))
                / Math.pow(10, precision);
        double roundedY =
            Math.round(rounded.getOrdinate(i, CoordinateSequence.Y) * Math.pow(10, precision))
                / Math.pow(10, precision);
        rounded.setOrdinate(i, CoordinateSequence.X, roundedX);
        rounded.setOrdinate(i, CoordinateSequence.Y, roundedY);
      }
      return rounded;
    }
  }
}
