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

package com.baremaps.postgres.copy;

import static org.locationtech.jts.io.WKBConstants.wkbNDR;

import de.bytefish.pgbulkinsert.pgsql.handlers.BaseValueHandler;
import java.io.DataOutputStream;
import java.io.IOException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;

/**
 * Encodes a JTS geometry as the EWKB a PostGIS geometry column reads.
 *
 * <p>
 * The writer is created per call rather than shared: {@code WKBWriter} keeps a buffer between
 * calls, and the repositories copy from several threads at once.
 */
public class GeometryValueHandler extends BaseValueHandler<Geometry> {

  /** Little endian, with the SRID included, which is what makes it EWKB rather than plain WKB. */
  private static byte[] asEwkb(Geometry geometry) {
    return new WKBWriter(2, wkbNDR, true).write(geometry);
  }

  @Override
  protected void internalHandle(DataOutputStream buffer, Geometry value) throws IOException {
    byte[] ewkb = asEwkb(value);
    buffer.writeInt(ewkb.length);
    buffer.write(ewkb, 0, ewkb.length);
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * Only a {@code CollectionValueHandler} asks for this, to size the elements of an array; the copy
   * path itself does not, which is why encoding the geometry twice is acceptable here.
   */
  @Override
  public int getLength(Geometry geometry) {
    return asEwkb(geometry).length + 4;
  }
}
