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

package com.baremaps.flatgeobuf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.flatgeobuf.FlatGeoBuf.Column;
import com.baremaps.flatgeobuf.FlatGeoBuf.ColumnType;
import com.baremaps.flatgeobuf.FlatGeoBuf.Crs;
import com.baremaps.flatgeobuf.FlatGeoBuf.Feature;
import com.baremaps.flatgeobuf.FlatGeoBuf.GeometryType;
import com.baremaps.flatgeobuf.FlatGeoBuf.Header;
import com.baremaps.testing.TestFiles;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

class FlatGeoBufTest {

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  @Test
  void copyPreservesHeaderAndFeatures() throws IOException {
    Path copy = Files.createTempFile("countries", ".fgb");

    Header header;
    List<Feature> features = new ArrayList<>();
    try (
        FlatGeoBufReader reader = reader(TestFiles.POINT_FLATGEOBUF);
        FlatGeoBufWriter writer = writer(copy)) {
      header = reader.readHeader();
      writer.writeHeader(header);
      writer.writeIndex(reader.readIndex());
      for (long i = 0; i < header.featuresCount(); i++) {
        Feature feature = reader.readFeature();
        writer.writeFeature(feature);
        features.add(feature);
      }
    }

    Content content = read(copy);
    assertEquals(header, content.header());
    assertEquals(features, content.features());
  }

  /**
   * The header used to be written with only half of its fields, so a file that was read and written
   * back lost its flags, its titles and its descriptions without saying so.
   */
  @Test
  void headerKeepsEveryField() throws IOException {
    Header header = new Header(
        "name",
        new Envelope(1, 2, 3, 4),
        GeometryType.POINT,
        true, true, true, true,
        List.of(new Column("column", ColumnType.INT, "column title", "column description",
            10, 2, 1, false, true, true, "column metadata")),
        0,
        0,
        new Crs("EPSG", 4326, "WGS 84", "crs description", "GEOGCS[]", "EPSG:4326"),
        "title",
        "description",
        "metadata");

    assertEquals(header, roundTrip(header, List.of()).header());
  }

  /** Neither the envelope nor the coordinate reference system is mandatory. */
  @Test
  void headerWithoutEnvelopeOrCrsIsReadBack() throws IOException {
    Header header = header(GeometryType.POINT, List.of(), 0);

    Header read = roundTrip(header, List.of()).header();

    assertNull(read.envelope());
    assertNull(read.crs());
    assertEquals(header, read);
  }

  /**
   * The format omits the properties a feature does not carry, so the values have to be kept on
   * their own columns rather than packed together: writing them back used to shift them onto the
   * wrong columns.
   */
  @Test
  void absentPropertiesStayOnTheirColumns() throws IOException {
    Header header = header(GeometryType.POINT, List.of(
        column("first", ColumnType.INT),
        column("second", ColumnType.STRING),
        column("third", ColumnType.DOUBLE)), 2);
    List<Feature> features = List.of(
        new Feature(Arrays.asList(null, "second value", 3.0), point(1, 1)),
        new Feature(Arrays.asList(1, null, null), point(2, 2)));

    Content content = roundTrip(header, features);

    assertEquals(features, content.features());
    assertEquals(Arrays.asList(null, "second value", 3.0), content.features().get(0).properties());
  }

  /** Every column type of the specification survives a round trip, in both directions. */
  @Test
  void everyColumnTypeIsWrittenAndReadBack() throws IOException {
    Header header = header(GeometryType.POINT, List.of(
        column("byte", ColumnType.BYTE),
        column("bool", ColumnType.BOOL),
        column("short", ColumnType.SHORT),
        column("int", ColumnType.INT),
        column("long", ColumnType.LONG),
        column("float", ColumnType.FLOAT),
        column("double", ColumnType.DOUBLE),
        column("string", ColumnType.STRING),
        column("json", ColumnType.JSON),
        column("datetime", ColumnType.DATETIME)), 1);
    List<Object> values = Arrays.asList(
        (byte) 1, true, (short) 2, 3, 4L, 5.0f, 6.0d, "sept", "{\"h\":8}", "2026-08-29T00:00:00Z");

    Content content = roundTrip(header, List.of(new Feature(values, point(1, 1))));

    assertEquals(values, content.features().get(0).properties());
  }

  /** Binary is the one column type whose values are neither a number nor a string. */
  @Test
  void binaryColumnIsWrittenAndReadBack() throws IOException {
    Header header = header(GeometryType.POINT, List.of(column("blob", ColumnType.BINARY)), 1);
    byte[] blob = {1, 2, 3, 4, 5};

    Content content = roundTrip(header, List.of(new Feature(List.of(blob), point(1, 1))));

    assertArrayEquals(blob, (byte[]) content.features().get(0).properties().get(0));
  }

  /**
   * A stream cannot be seeked over, so the index has to be read and discarded rather than jumped
   * over. Both ways of getting past it have to land on the first feature.
   */
  @Test
  void indexIsSkippedOnAChannelThatCannotSeek() throws IOException {
    try (FlatGeoBufReader reader = new FlatGeoBufReader(
        Channels.newChannel(Files.newInputStream(TestFiles.POINT_FLATGEOBUF)))) {
      Header header = reader.readHeader();
      reader.skipIndex();
      assertEquals(read(TestFiles.POINT_FLATGEOBUF).features().get(0), reader.readFeature());
      assertEquals(GeometryType.MULTIPOLYGON, header.geometryType());
    }
  }

  /**
   * A file that declares no geometry type carries one per feature. Reading it used to return no
   * geometry at all.
   */
  @Test
  void fileWithoutADeclaredGeometryTypeKeepsItsGeometries() throws IOException {
    Header header = header(GeometryType.UNKNOWN, List.of(), 3);
    List<Feature> features = List.of(
        feature(point(1, 2)),
        feature(GEOMETRY_FACTORY.createLineString(
            new Coordinate[] {new Coordinate(0, 0), new Coordinate(1, 1)})),
        feature(GEOMETRY_FACTORY.createPolygon(new Coordinate[] {
            new Coordinate(0, 0), new Coordinate(1, 0),
            new Coordinate(1, 1), new Coordinate(0, 0)})));

    Content content = roundTrip(header, features);

    assertEquals(features, content.features());
  }

  /** The header decides whether a Z ordinate is stored, and it has to survive the file. */
  @Test
  void headerThatDeclaresZKeepsIt() throws IOException {
    Header header = new Header(null, null, GeometryType.POINT, true, false, false, false,
        List.of(), 1, 0, null, null, null, null);
    Geometry point = GEOMETRY_FACTORY.createPoint(new Coordinate(1, 2, 3));

    Content content = roundTrip(header, List.of(feature(point)));

    assertTrue(content.header().hasZ());
    assertEquals(3.0, content.features().get(0).geometry().getCoordinate().getZ());
  }

  /** A 2D file must not be padded with a vector of NaN, which JTS invites by reporting a Z. */
  @Test
  void headerThatDeclaresNoZDropsIt() throws IOException {
    Header header = header(GeometryType.POINT, List.of(), 1);
    Geometry point = GEOMETRY_FACTORY.createPoint(new Coordinate(1, 2, 3));

    Content content = roundTrip(header, List.of(feature(point)));

    assertTrue(Double.isNaN(content.features().get(0).geometry().getCoordinate().getZ()));
  }

  @Test
  void headerThatDeclaresMeasuresKeepsThem() throws IOException {
    Header header = new Header(null, null, GeometryType.POINT, true, true, false, false,
        List.of(), 1, 0, null, null, null, null);
    Geometry point = GEOMETRY_FACTORY.createPoint(new CoordinateXYZM(1, 2, 3, 4));

    Content content = roundTrip(header, List.of(feature(point)));

    assertEquals(4.0, content.features().get(0).geometry().getCoordinate().getM());
  }

  @Test
  void polygonWithHolesKeepsItsRings() throws IOException {
    Header header = header(GeometryType.POLYGON, List.of(), 1);
    Geometry polygon = GEOMETRY_FACTORY.createPolygon(
        GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
            new Coordinate(0, 0), new Coordinate(10, 0),
            new Coordinate(10, 10), new Coordinate(0, 10), new Coordinate(0, 0)}),
        new org.locationtech.jts.geom.LinearRing[] {
            GEOMETRY_FACTORY.createLinearRing(new Coordinate[] {
                new Coordinate(1, 1), new Coordinate(2, 1),
                new Coordinate(2, 2), new Coordinate(1, 1)})});

    Content content = roundTrip(header, List.of(feature(polygon)));

    assertEquals(polygon, content.features().get(0).geometry());
  }

  @Test
  void featureWithoutGeometryIsReadBack() throws IOException {
    Header header = header(GeometryType.POINT, List.of(column("value", ColumnType.INT)), 1);

    Content content = roundTrip(header, List.of(new Feature(List.of(1), null)));

    assertNull(content.features().get(0).geometry());
  }

  @Test
  void readingBeforeTheHeaderIsRejected() throws IOException {
    try (FlatGeoBufReader reader = reader(TestFiles.POINT_FLATGEOBUF)) {
      assertThrows(IllegalStateException.class, reader::readFeature);
      assertThrows(IllegalStateException.class, reader::skipIndex);
    }
  }

  @Test
  void writingBeforeTheHeaderIsRejected() throws IOException {
    Path file = Files.createTempFile("empty", ".fgb");
    try (FlatGeoBufWriter writer = writer(file)) {
      assertThrows(IllegalStateException.class,
          () -> writer.writeFeature(new Feature(List.of(), null)));
    }
  }

  @Test
  void fileThatIsNotFlatGeoBufIsRejected() throws IOException {
    Path file = Files.createTempFile("not", ".fgb");
    Files.write(file, new byte[64]);

    try (FlatGeoBufReader reader = reader(file)) {
      assertThrows(IOException.class, reader::readHeader);
    }
  }

  @Test
  void truncatedFileIsRejected() throws IOException {
    Path file = Files.createTempFile("truncated", ".fgb");
    Files.write(file, Arrays.copyOf(Files.readAllBytes(TestFiles.POINT_FLATGEOBUF), 6));

    try (FlatGeoBufReader reader = reader(file)) {
      assertThrows(IOException.class, reader::readHeader);
    }
  }

  /** A feature larger than the read buffer has to be read whole rather than in pieces. */
  @Test
  void featureLargerThanTheReadBufferIsReadBack() throws IOException {
    Header header = header(GeometryType.LINESTRING, List.of(), 1);
    Coordinate[] coordinates = new Coordinate[20_000];
    for (int i = 0; i < coordinates.length; i++) {
      coordinates[i] = new Coordinate(i, i);
    }
    Geometry line = GEOMETRY_FACTORY.createLineString(coordinates);

    Content content = roundTrip(header, List.of(feature(line)));

    assertEquals(line, content.features().get(0).geometry());
  }

  private record Content(Header header, List<Feature> features) {
  }

  private Content roundTrip(Header header, List<Feature> features) throws IOException {
    Path file = Files.createTempFile("roundtrip", ".fgb");
    try (FlatGeoBufWriter writer = writer(file)) {
      writer.writeHeader(header);
      for (Feature feature : features) {
        writer.writeFeature(feature);
      }
    }
    return read(file);
  }

  private Content read(Path file) throws IOException {
    try (FlatGeoBufReader reader = reader(file)) {
      Header header = reader.readHeader();
      reader.skipIndex();
      List<Feature> features = new ArrayList<>();
      for (long i = 0; i < header.featuresCount(); i++) {
        features.add(reader.readFeature());
      }
      assertNotNull(features);
      return new Content(header, features);
    }
  }

  private static FlatGeoBufReader reader(Path file) throws IOException {
    return new FlatGeoBufReader(FileChannel.open(file, StandardOpenOption.READ));
  }

  private static FlatGeoBufWriter writer(Path file) throws IOException {
    return new FlatGeoBufWriter(FileChannel.open(file,
        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
  }

  private static Header header(GeometryType type, List<Column> columns, long featuresCount) {
    return new Header(null, null, type, false, false, false, false, columns, featuresCount, 0,
        null, null, null, null);
  }

  private static Column column(String name, ColumnType type) {
    return new Column(name, type, null, null, 0, 0, 0, true, false, false, null);
  }

  private static Feature feature(Geometry geometry) {
    return new Feature(List.of(), geometry);
  }

  private static Geometry point(double x, double y) {
    return GEOMETRY_FACTORY.createPoint(new Coordinate(x, y));
  }

}
