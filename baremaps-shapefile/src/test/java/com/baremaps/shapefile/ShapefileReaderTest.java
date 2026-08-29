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

package com.baremaps.shapefile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.shapefile.Shapefile.Column;
import com.baremaps.shapefile.Shapefile.ColumnType;
import com.baremaps.shapefile.Shapefile.GeometryType;
import com.baremaps.testing.TestFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Point;

class ShapefileReaderTest {

  private static final Path POINT = TestFiles.POINT_SHP;

  @Test
  void readsTheColumnsOfTheTable() throws Exception {
    try (ShapefileReader reader = new ShapefileReader(POINT)) {
      assertEquals(List.of("id", "text", "integer", "float", "date"),
          reader.columns().stream().map(Column::name).toList());
      assertEquals(List.of(Long.class, String.class, Long.class, Double.class, LocalDate.class),
          reader.columns().stream().map(Column::javaType).toList());
      assertEquals(ColumnType.CHARACTER, reader.columns().get(1).type());
    }
  }

  @Test
  void readsTheHeaderOfTheShapefile() throws Exception {
    try (ShapefileReader reader = new ShapefileReader(POINT)) {
      assertEquals(GeometryType.POINT, reader.header().geometryType());
      assertTrue(reader.header().envelope().contains(-38.52418057671082, -13.059103786963561));
      assertTrue(reader.header().envelope().contains(2.118706969903883, 42.50183785913111));
    }
  }

  @Test
  void readsTheFeatures() throws Exception {
    try (ShapefileReader reader = new ShapefileReader(POINT)) {
      List<Object> first = reader.readRow();
      assertEquals(List.of(1L, "text1", 10L, 20.0d, LocalDate.of(2023, 10, 27)),
          first.subList(0, 5));
      Point geometry = assertInstanceOf(Point.class, first.get(5));
      assertEquals(-38.52418057671082, geometry.getX());

      assertEquals(2L, reader.readRow().get(0));
      assertNull(reader.readRow());
    }
  }

  @Test
  void readsAShapefileThatHasNoIndex(@TempDir Path directory) throws Exception {
    // The .shx index is optional, and the reader goes through the records in order anyway.
    Path shapefile = copy(directory, "point", ".shp", ".dbf");
    assertEquals(rows(POINT), rows(shapefile));
  }

  @Test
  void findsTheCompanionFilesWhateverTheCaseOfTheirExtension(@TempDir Path directory)
      throws Exception {
    Path shapefile = directory.resolve("POINT.SHP");
    Files.copy(POINT, shapefile);
    Files.copy(sibling(POINT, ".dbf"), directory.resolve("POINT.DBF"));
    assertEquals(rows(POINT), rows(shapefile));
  }

  @Test
  void takesTheCharsetFromTheCpgSidecar(@TempDir Path directory) throws Exception {
    Path shapefile = copy(directory, "point", ".shp", ".dbf");

    // EBCDIC maps the bytes of the attributes to other characters entirely, which is what shows
    // the sidecar decides the charset rather than being ignored.
    Files.writeString(directory.resolve("point.cpg"), "IBM500");
    try (ShapefileReader reader = new ShapefileReader(shapefile)) {
      assertNotEquals("text1", reader.readRow().get(1));
    }

    Files.writeString(directory.resolve("point.cpg"), "UTF-8");
    try (ShapefileReader reader = new ShapefileReader(shapefile)) {
      assertEquals("text1", reader.readRow().get(1));
    }
  }

  @Test
  void failsOnAMissingTable(@TempDir Path directory) throws Exception {
    Path shapefile = copy(directory, "point", ".shp");
    assertThrows(NoSuchFileException.class, () -> new ShapefileReader(shapefile));
  }

  @Test
  void failsOnAFileThatIsNotAShapefile(@TempDir Path directory) throws Exception {
    Path shapefile = directory.resolve("broken.shp");
    Files.write(shapefile, new byte[200]);
    Files.copy(sibling(POINT, ".dbf"), directory.resolve("broken.dbf"));
    assertThrows(ShapefileException.class, () -> new ShapefileReader(shapefile));
  }

  @Test
  void closesMoreThanOnceWithoutComplaining() throws Exception {
    ShapefileReader reader = new ShapefileReader(POINT);
    reader.close();
    assertDoesNotThrow(reader::close);
  }

  @Test
  void failsToReadOnceClosed() throws Exception {
    ShapefileReader reader = new ShapefileReader(POINT);
    reader.close();
    assertThrows(IllegalStateException.class, reader::readRow);
  }

  /** Copies the named companions of the fixture into a directory of their own. */
  private static Path copy(Path directory, String name, String... extensions) throws IOException {
    for (String extension : extensions) {
      Files.copy(sibling(POINT, extension), directory.resolve(name + extension));
    }
    return directory.resolve(name + extensions[0]);
  }

  private static Path sibling(Path shapefile, String extension) {
    String name = shapefile.getFileName().toString();
    return shapefile.resolveSibling(name.substring(0, name.lastIndexOf('.'))
        + extension.toLowerCase(Locale.ROOT));
  }

  private static List<List<Object>> rows(Path shapefile) throws IOException {
    try (ShapefileReader reader = new ShapefileReader(shapefile)) {
      List<List<Object>> rows = new ArrayList<>();
      List<Object> row;
      while ((row = reader.readRow()) != null) {
        rows.add(row);
      }
      return rows;
    }
  }
}
