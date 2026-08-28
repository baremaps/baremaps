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



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Input Stream of features.
 *
 * <p>
 * <div class="warning">This is an experimental class, not yet target for any Apache SIS release at
 * this time.</div>
 *
 * @author Marc Le Bihan
 */
public class ShapefileInputStream extends InputStream {

  private DbaseByteReader dbaseReader;

  /** Shapefile. */
  private File shapefile;

  /** Database file. */
  private File databaseFile;

  /** Shapefile index. */
  private File shapefileIndex;

  /** Indicates that the shape file has a valid index provided with it. */
  private boolean hasShapefileIndex;

  /** Row type of the features contained in this shapefile. */
  // private DataSchema schema;

  /** Shapefile reader. */
  private ShapefileByteReader shapefileReader;

  /**
   * Create an input stream of features over a connection.
   *
   * @param shapefile Shapefile.
   * @param dbaseFile Database file.
   * @param shpfileIndex Shapefile index, null if none provided, will be checked for existence.
   */
  public ShapefileInputStream(File shapefile, File dbaseFile, File shpfileIndex)
      throws IOException {
    this.shapefile = shapefile;
    this.databaseFile = dbaseFile;
    this.dbaseReader = new DbaseByteReader(dbaseFile, null);

    if (shpfileIndex != null && (shpfileIndex.exists() && shpfileIndex.isFile())) {
      this.shapefileIndex = shpfileIndex;
      this.hasShapefileIndex = true;
    } else {
      this.hasShapefileIndex = false;
    }

    this.shapefileReader =
        new ShapefileByteReader(this.shapefile, this.databaseFile, this.shapefileIndex);
  }

  /**
   * Create an input stream of features over a connection, responding to a SELECT * FROM DBF
   * statement.
   *
   * @param shpfile Shapefile.
   * @param dbaseFile Database file.
   */
  public ShapefileInputStream(File shpfile, File dbaseFile) throws IOException {
    this(shpfile, dbaseFile, null);
  }

  /** @see java.io.InputStream#read() */
  @Override
  public int read() {
    throw new UnsupportedOperationException(
        "InputFeatureStream doesn't allow the use of read(). Use readFeature() instead.");
  }

  /** @see java.io.InputStream#available() */
  @Override
  public int available() {
    throw new UnsupportedOperationException(
        "InputFeatureStream doesn't allow the use of available(). Use readFeature() will return null when feature are no more available.");
  }

  /** @see java.io.InputStream#close() */
  @Override
  public void close() throws IOException {
    IOException firstException = null;

    try {
      if (this.dbaseReader != null) {
        this.dbaseReader.close();
        this.dbaseReader = null;
      }
    } catch (IOException e) {
      firstException = e;
    }

    try {
      if (this.shapefileReader != null) {
        this.shapefileReader.close();
        this.shapefileReader = null;
      }
    } catch (IOException e) {
      if (firstException == null) {
        firstException = e;
      } else {
        firstException.addSuppressed(e);
      }
    }

    if (firstException != null) {
      throw firstException;
    }
  }

  /**
   * Read next feature responding to the SQL query.
   *
   * @return Feature, null if no more feature is available.
   * @throws ShapefileException if the current connection used to query the shapefile has been
   *         closed.
   */
  public List<Object> readRow() throws ShapefileException {
    if (!this.dbaseReader.nextRowAvailable()) {
      return null;
    }
    List<Object> row = new ArrayList<>();
    this.dbaseReader.loadRow(row);
    this.shapefileReader.setRowNum(this.dbaseReader.getRowNum());
    this.shapefileReader.completeRow(row);
    return row;
  }

  /**
   * Returns the schema.
   *
   * @return the schema.
   */
  // public DataSchema schema() {
  // return schema;
  // }

  /**
   * Returns the shapefile descriptor.
   *
   * @return Shapefile descriptor.
   */
  public ShapefileDescriptor getShapefileDescriptor() {
    return this.shapefileReader.getShapefileDescriptor();
  }

  /**
   * Returns the database fields descriptors.
   *
   * @return List of fields descriptors.
   */
  public List<DBaseFieldDescriptor> getDatabaseFieldsDescriptors() {
    return this.shapefileReader.getFieldsDescriptors();
  }

  /**
   * Checks if the shapefile has an index provided with it.
   *
   * @return true if an index file (.shx) has been given with the shapefile.
   */
  public boolean hasShapefileIndex() {
    return this.hasShapefileIndex;
  }

}
