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

import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Reads the features of a shapefile, one after the other.
 * <p>
 * A shapefile is a set of files that share a name: the {@code .shp} file holds the geometries, the
 * {@code .dbf} table holds the attributes, and one record of each makes one feature. This reader
 * takes the path of the {@code .shp} file and finds its companions itself, since the specification
 * is what relates them and no caller has a say in it.
 * <p>
 * Both files are memory mapped, which the specification allows for by capping either of them at two
 * gigabytes. The mappings are released by {@link #close()}, so a reader that is not closed keeps
 * the files open for as long as it takes the garbage collector to notice it.
 * <p>
 * A reader holds the position it has read up to and is therefore not safe for use by several
 * threads at once.
 */
public class ShapefileReader implements AutoCloseable {

  private final Arena arena;

  private final GeometryDecoder geometries;

  private final DbaseDecoder attributes;

  /**
   * Opens a shapefile, taking its charset from the files themselves and building its geometries
   * with a default {@link GeometryFactory}.
   *
   * @param shapefile the path of the {@code .shp} file
   */
  public ShapefileReader(Path shapefile) throws IOException {
    this(shapefile, new GeometryFactory(), null);
  }

  /**
   * Opens a shapefile.
   *
   * @param shapefile the path of the {@code .shp} file
   * @param geometryFactory the factory that builds the geometries, which is how a caller imposes an
   *        SRID, a precision model or a coordinate sequence implementation
   * @param charset the charset of the text of the attributes, or null to take the one the files
   *        declare: the {@code .cpg} sidecar if there is one, the code page of the table otherwise
   */
  public ShapefileReader(Path shapefile, GeometryFactory geometryFactory, Charset charset)
      throws IOException {
    this.arena = Arena.ofShared();
    try {
      this.geometries = new GeometryDecoder(map(shapefile), geometryFactory);
      this.attributes = new DbaseDecoder(map(sibling(shapefile, "dbf")),
          charset != null ? charset : declaredCharset(shapefile));
    } catch (IOException | RuntimeException e) {
      arena.close();
      throw e;
    }
  }

  /** Returns the header of the shapefile, which describes the whole of its content. */
  public Shapefile.Header header() {
    return geometries.header();
  }

  /** Returns the columns of the attributes, in the order {@link #readRow()} returns them. */
  public List<Shapefile.Column> columns() {
    return attributes.columns();
  }

  /**
   * Reads the next feature as the values of its {@link #columns() columns}, in column order,
   * followed by its geometry. A value is null where the record leaves the field blank, and the
   * geometry is null where the file records the feature as having none.
   *
   * @return the values of the feature, or null once the file holds no further feature
   */
  public List<Object> readRow() throws ShapefileException {
    DbaseDecoder.Row row;
    Geometry geometry;
    do {
      row = attributes.next();
      if (row == null) {
        return null;
      }
      // Both files hold one record per feature, so the geometry of a deleted record is read and
      // dropped rather than skipped, which is what keeps the two files in step.
      geometry = geometries.next();
    } while (row.deleted());

    List<Object> values = new ArrayList<>(row.values().size() + 1);
    values.addAll(row.values());
    values.add(geometry);
    return values;
  }

  /**
   * Releases the mappings of the files. Reading from a closed reader fails. Closing a reader twice
   * does not, since a caller that closes one on its way out of a loop and again in a finally block
   * has done nothing wrong.
   */
  @Override
  public void close() {
    if (arena.scope().isAlive()) {
      arena.close();
    }
  }

  private ByteBuffer map(Path path) throws IOException {
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
      long size = channel.size();
      if (size > Integer.MAX_VALUE) {
        throw new ShapefileException(
            "%s holds more than the two gigabytes a shapefile may hold".formatted(path));
      }
      // The mapping outlives the channel: the arena, not the channel, decides when it is unmapped.
      return channel.map(MapMode.READ_ONLY, 0, size, arena).asByteBuffer();
    }
  }

  /**
   * The companion of the shapefile that carries the given extension. Both cases are tried, because
   * a file named {@code POINT.SHP} sits next to {@code POINT.DBF} rather than next to
   * {@code POINT.dbf}. The lower case one is returned when neither exists, so that the failure
   * names the file a reader would ordinarily expect.
   */
  private static Path sibling(Path shapefile, String extension) {
    String name = shapefile.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String base = dot < 0 ? name : name.substring(0, dot);
    Path lower = shapefile.resolveSibling(base + "." + extension);
    Path upper = shapefile.resolveSibling(base + "." + extension.toUpperCase(Locale.ROOT));
    return !Files.isRegularFile(lower) && Files.isRegularFile(upper) ? upper : lower;
  }

  /**
   * The charset the {@code .cpg} sidecar names, or null when there is no intelligible one to read.
   * The sidecar is how the tools that write a shapefile record an encoding the code page of the
   * table cannot express, UTF-8 above all, and it therefore has the last word over the code page.
   */
  private static Charset declaredCharset(Path shapefile) {
    try {
      String name = Files.readString(sibling(shapefile, "cpg"), StandardCharsets.US_ASCII).strip();
      return Charset.forName(name);
    } catch (IOException | RuntimeException e) {
      // A sidecar that is missing, unreadable, or names something other than a charset, such as a
      // bare code page number, leaves the decision to the table.
      return null;
    }
  }
}
