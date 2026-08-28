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

public class GeometryValueHandler extends BaseValueHandler<Geometry> {

  private static byte[] asWKB(Geometry geometry) {
    return new WKBWriter(2, wkbNDR, true).write(geometry);
  }

  @Override
  protected void internalHandle(DataOutputStream buffer, Geometry value) throws IOException {
    byte[] wkb = asWKB(value);
    buffer.writeInt(wkb.length);
    buffer.write(wkb, 0, wkb.length);
  }

  @Override
  public int getLength(Geometry geometry) {
    return asWKB(geometry).length + 4;
  }
}
