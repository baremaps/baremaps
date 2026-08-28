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



import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;

/**
 * Shapefile Descriptor.
 *
 * @author Marc Le Bihan
 */
public class ShapefileDescriptor {
  /** File code. */
  private int fileCode; // big

  /** File length. */
  private int fileLength; // big // The value for file length is the total length of the file in
                          // 16-bit
  // words

  /** File version. */
  private int version; // little

  /** Shapefile type. */
  private ShapefileGeometryType shapefileGeometryType; // little

  /** X Min. */
  private double xmin; // little

  /** Y Min. */
  private double ymin; // little

  /** X Max. */
  private double xmax; // little

  /** Y Max. */
  private double ymax; // little

  /** Z Min. */
  private double zmin; // little

  /** Z Max. */
  private double zmax; // little

  /** M Min. */
  private double mmin; // little

  /** M Max. */
  private double mmax; // little

  /**
   * Create a shapefile descriptor.
   *
   * @param byteBuffer Source Bytebuffer.
   */
  public ShapefileDescriptor(MappedByteBuffer byteBuffer) {
    this.fileCode = byteBuffer.getInt();
    byteBuffer.getInt();
    byteBuffer.getInt();
    byteBuffer.getInt();
    byteBuffer.getInt();
    byteBuffer.getInt();
    this.fileLength = byteBuffer.getInt() * 2;

    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    this.version = byteBuffer.getInt();
    this.shapefileGeometryType = ShapefileGeometryType.get(byteBuffer.getInt());
    this.xmin = byteBuffer.getDouble();
    this.ymin = byteBuffer.getDouble();
    this.xmax = byteBuffer.getDouble();
    this.ymax = byteBuffer.getDouble();
    this.zmin = byteBuffer.getDouble();
    this.zmax = byteBuffer.getDouble();
    this.mmin = byteBuffer.getDouble();
    this.mmax = byteBuffer.getDouble();
    byteBuffer.order(ByteOrder.BIG_ENDIAN);

    // dbf.byteBuffer.get(); // should be 0d for field terminator
  }

  /**
   * Returns the version of the shapefile.
   *
   * @return Version.
   */
  public int getVersion() {
    return this.version;
  }

  /**
   * Returns the ESRI shape type in the shapefile.
   *
   * @return Shape type.
   */
  public ShapefileGeometryType getShapeType() {
    return this.shapefileGeometryType;
  }

  /**
   * Returns the X Min property.
   *
   * @return XMin.
   */
  public double getXmin() {
    return this.xmin;
  }

  /**
   * Returns the Y Min property.
   *
   * @return YMin.
   */
  public double getYmin() {
    return this.ymin;
  }

  /**
   * Returns the X Max property.
   *
   * @return XMax.
   */
  public double getXmax() {
    return this.xmax;
  }

  /**
   * Returns the Y Max property.
   *
   * @return YMax.
   */
  public double getYmax() {
    return this.ymax;
  }

  /**
   * Returns the Z Min property.
   *
   * @return ZMin.
   */
  public double getZmin() {
    return this.zmin;
  }

  /**
   * Returns the Z Max property.
   *
   * @return ZMax.
   */
  public double getZmax() {
    return this.zmax;
  }

  /**
   * Returns the M Min property.
   *
   * @return M min.
   */
  public double getMmin() {
    return this.mmin;
  }

  /**
   * Returns the M Max property.
   *
   * @return M Max.
   */
  public double getMmax() {
    return this.mmax;
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    String lineSeparator = System.lineSeparator();

    s.append("FileCode: ").append(this.fileCode).append(lineSeparator);
    s.append("FileLength: ").append(this.fileLength).append(lineSeparator);
    s.append("Version: ").append(this.version).append(lineSeparator);
    s.append("ShapeType: ").append(this.shapefileGeometryType).append(lineSeparator);
    s.append("xmin: ").append(this.xmin).append(lineSeparator);
    s.append("ymin: ").append(this.ymin).append(lineSeparator);
    s.append("xmax: ").append(this.xmax).append(lineSeparator);
    s.append("ymax: ").append(this.ymax).append(lineSeparator);
    s.append("zmin: ").append(this.zmin).append(lineSeparator);
    s.append("zmax: ").append(this.zmax).append(lineSeparator);
    s.append("mmin: ").append(this.mmin).append(lineSeparator);
    s.append("mmax: ").append(this.mmax).append(lineSeparator);
    s.append("------------------------").append(lineSeparator);

    return s.toString();
  }
}
