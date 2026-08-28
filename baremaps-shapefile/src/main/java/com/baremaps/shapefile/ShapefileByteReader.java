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



import java.io.*;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Reader of a Shapefile Binary content by the way of a {@link java.nio.MappedByteBuffer}
 *
 * @author Marc Le Bihan
 */
public class ShapefileByteReader extends CommonByteReader {

  /** Name of the Geometry field. */
  private static final String GEOMETRY_NAME = "geometry";

  /** Shapefile descriptor. */
  private ShapefileDescriptor shapefileDescriptor;

  /** Database Field descriptors. */
  private List<DBaseFieldDescriptor> databaseFieldsDescriptors;

  /** Shapefile index. */
  private File shapeFileIndex;

  /** Shapefile indexes (loaded from .SHX file, if any found). */
  private ArrayList<Integer> indexes;

  /** Shapefile records lengths (loaded from .SHX file, if any found). */
  private ArrayList<Integer> recordsLengths;

  /** JTS geometry factory. */
  private GeometryFactory geometryFactory = new GeometryFactory();

  /**
   * Construct a shapefile byte reader.
   *
   * @param shapefile Shapefile.
   * @param dbaseFile underlying database file name.
   * @param shapefileIndex Shapefile index, if any. Null else.
   * @throws DbaseException if the database file format is invalid.
   * @throws ShapefileException if the shapefile has not been found.
   */
  public ShapefileByteReader(File shapefile, File dbaseFile, File shapefileIndex)
      throws IOException {
    super(shapefile);
    this.shapeFileIndex = shapefileIndex;

    loadDatabaseFieldDescriptors(dbaseFile);
    loadDescriptor();

    if (this.shapeFileIndex != null) {
      loadShapefileIndexes();
    }

    // this.schema = getSchema(shapefile.getName());
  }

  /**
   * Returns the DBase 3 columns descriptors.
   *
   * @return Fields descriptors.
   */
  public List<DBaseFieldDescriptor> getFieldsDescriptors() {
    return this.databaseFieldsDescriptors;
  }

  /**
   * Returns the shapefile descriptor.
   *
   * @return Shapefile descriptor.
   */
  public ShapefileDescriptor getShapefileDescriptor() {
    return this.shapefileDescriptor;
  }

  /** Load shapefile descriptor. */
  private void loadDescriptor() {
    this.shapefileDescriptor = new ShapefileDescriptor(getByteBuffer());
  }

  /**
   * Load shapefile indexes.
   *
   * @return true if shapefile indexes has been read, false if none where available or a problem
   *         occured.
   */
  private boolean loadShapefileIndexes() {
    if (this.shapeFileIndex == null) {
      return false;
    }

    try (FileInputStream fis = new FileInputStream(this.shapeFileIndex);
        FileChannel fc = fis.getChannel()) {
      int fsize = (int) fc.size();
      MappedByteBuffer indexesByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fsize);

      // Indexes entries follow.
      this.indexes = new ArrayList<>();
      this.recordsLengths = new ArrayList<>();
      indexesByteBuffer.position(100);
      indexesByteBuffer.order(ByteOrder.BIG_ENDIAN);

      while (indexesByteBuffer.hasRemaining()) {
        this.indexes.add(indexesByteBuffer.getInt()); // Data offset : the position of the record
                                                      // in the main shapefile,
        // expressed in words (16 bits).
        this.recordsLengths.add(indexesByteBuffer.getInt()); // Length of this shapefile record.
      }
      return true;
    } catch (IOException e) {
      this.shapeFileIndex = null;
      return false;
    }
  }

  /**
   * Load database field descriptors.
   *
   * @param dbaseFile Database file.
   * @throws DbaseException if the database format is incorrect.
   */
  private void loadDatabaseFieldDescriptors(File dbaseFile) throws IOException {
    DbaseByteReader databaseReader = null;
    try {
      databaseReader = new DbaseByteReader(dbaseFile, null);
      this.databaseFieldsDescriptors = databaseReader.getFieldsDescriptors();
    } finally {
      if (databaseReader != null) {
        try {
          databaseReader.close();
        } catch (IOException e) {
          // Ignore
        }
      }
    }
  }

  /**
   * Direct access to a row by its record number.
   *
   * @param recordNumber Record number.
   */
  public void setRowNum(int recordNumber) throws ShapefileException {
    // Check that the asked record number is not before the first.
    if (recordNumber < 1) {
      throw new IllegalArgumentException("Wrong direct access before start");
    }

    // Check that the shapefile allows direct access : it won't if it has no index.
    if (this.shapeFileIndex == null) {
      throw new ShapefileException("No direct access");
    }

    int position = this.indexes.get(recordNumber - 1) * 2; // Indexes unit are words (16 bits).

    // Check that the asked record number is not after the last.
    if (position >= this.getByteBuffer().capacity()) {
      throw new ShapefileException("Wrong direct access after last");
    }

    try {
      getByteBuffer().position(position);
    } catch (IllegalArgumentException e) {
      throw new ShapefileException("Wrong position", e);
    }
  }

  /**
   * Complete a row with shapefile content.
   *
   * @param row the row to complete
   */
  public void completeRow(List<Object> row) throws ShapefileException {
    // insert points into some type of list
    getByteBuffer().getInt(); // record number
    getByteBuffer().getInt(); // content length

    getByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
    int shapeTypeId = getByteBuffer().getInt();

    ShapefileGeometryType shapefileGeometryType = ShapefileGeometryType.get(shapeTypeId);

    if (shapefileGeometryType == null) {
      throw new ShapefileException(
          "The shapefile schema doesn''t match to any known schema.");
    }

    switch (shapefileGeometryType) {
      case NULL_SHAPE:
        loadNullRow(row);
        break;

      case POINT:
        loadPointRow(row);
        break;

      case POLYGON:
        loadPolygonRow(row);
        break;

      case POLY_LINE:
        loadPolylineRow(row);
        break;

      default:
        throw new ShapefileException("Unsupported shapefile type: " + shapeTypeId);
    }

    getByteBuffer().order(ByteOrder.BIG_ENDIAN);
  }

  /**
   * Load null row.
   *
   * @param row the row to fill.
   */
  private void loadNullRow(List<Object> row) {
    row.add(null);
  }

  /**
   * Load point row.
   *
   * @param row the row to fill.
   */
  private void loadPointRow(List<Object> row) {
    double x = getByteBuffer().getDouble();
    double y = getByteBuffer().getDouble();
    Point pnt = geometryFactory.createPoint(new Coordinate(x, y));
    row.add(pnt);
  }

  /**
   * Load polygon row.
   *
   * @param row the row to fill.
   */
  private void loadPolygonRow(List<Object> row) {
    /* double xmin = */ getByteBuffer().getDouble();
    /* double ymin = */ getByteBuffer().getDouble();
    /* double xmax = */ getByteBuffer().getDouble();
    /* double ymax = */ getByteBuffer().getDouble();

    int numParts = getByteBuffer().getInt();
    int numPoints = getByteBuffer().getInt();

    Geometry multiPolygon = readMultiplePolygon(numParts, numPoints);

    row.add(multiPolygon);
  }

  /**
   * Read a polygon that has multiple parts.
   *
   * @param numParts Number of parts of this polygon.
   * @param numPoints Total number of points of this polygon, all parts considered.
   * @return a multiple part polygon.
   */
  private Geometry readMultiplePolygon(int numParts, int numPoints) {
    /**
     * From ESRI Specification : Parts : 0 5 (meaning : 0 designs the first v1, 5 designs the first
     * v5 on the points list below). Points : v1 v2 v3 v4 v1 v5 v8 v7 v6 v5
     *
     * <p>
     * POSITION FIELD VALUE TYPE NUMBER ORDER Byte 0 Shape Type 5 Integer 1 Little Byte 4 Box Box
     * Double 4 Little Byte 36 NumParts NumParts Integer 1 Little Byte 40 NumPoints NumPoints
     * Integer 1 Little Byte 44 Parts Parts Integer NumParts Little Byte X Points Points Point
     * NumPoints Little
     */

    // Read all the parts indexes (starting at byte 44).
    var partsIndexes = new int[numParts];
    for (int index = 0; index < numParts; index++) {
      partsIndexes[index] = getByteBuffer().getInt();
    }

    // Read all the coordinates.
    var coordinates = new Coordinate[numPoints];
    for (var i = 0; i < numPoints; i++) {
      var x = getByteBuffer().getDouble();
      var y = getByteBuffer().getDouble();
      var coordinate = new Coordinate(x, y);
      coordinates[i] = coordinate;
    }

    // Create the shells and holes.
    var shells = new ArrayList<Polygon>();
    var holes = new LinkedList<Polygon>();
    for (var i = 0; i < partsIndexes.length; i++) {
      var from = partsIndexes[i];
      var to = i < partsIndexes.length - 1 ? partsIndexes[i + 1] : coordinates.length;
      var array = Arrays.copyOfRange(coordinates, from, to);
      var linearRing = geometryFactory.createPolygon(array);
      if (!Orientation.isCCW(linearRing.getCoordinates())) {
        shells.add(linearRing);
      } else {
        holes.add(linearRing);
      }
    }

    // Compute the difference between shells and holes
    var shellsMultiPolygon =
        geometryFactory.createMultiPolygon(shells.toArray(size -> new Polygon[size]));
    var holesMultiPolygon =
        geometryFactory.createMultiPolygon(holes.toArray(size -> new Polygon[size]));
    return shellsMultiPolygon.difference(holesMultiPolygon);
  }

  /**
   * Load polyline row.
   *
   * @param row the row to fill.
   */
  private void loadPolylineRow(List<Object> row) {
    /* double xmin = */ getByteBuffer().getDouble();
    /* double ymin = */ getByteBuffer().getDouble();
    /* double xmax = */ getByteBuffer().getDouble();
    /* double ymax = */ getByteBuffer().getDouble();

    int numParts = getByteBuffer().getInt();
    int numPoints = getByteBuffer().getInt();

    int[] numPartArr = new int[numParts + 1];

    for (int n = 0; n < numParts; n++) {
      int idx = getByteBuffer().getInt();
      numPartArr[n] = idx;
    }
    numPartArr[numParts] = numPoints;

    double xpnt;
    double ypnt;
    var coordinates = new CoordinateList();

    for (int m = 0; m < numParts; m++) {
      xpnt = getByteBuffer().getDouble();
      ypnt = getByteBuffer().getDouble();
      coordinates.add(new Coordinate(xpnt, ypnt));

      for (int j = numPartArr[m]; j < numPartArr[m + 1] - 1; j++) {
        xpnt = getByteBuffer().getDouble();
        ypnt = getByteBuffer().getDouble();
        coordinates.add(new Coordinate(xpnt, ypnt));
      }
    }

    row.add(geometryFactory.createLineString(coordinates.toCoordinateArray()));
  }
}
