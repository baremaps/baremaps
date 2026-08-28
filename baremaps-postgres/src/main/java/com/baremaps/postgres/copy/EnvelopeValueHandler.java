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
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKBWriter;

public class EnvelopeValueHandler extends BaseValueHandler<Envelope> {

  private static final GeometryFactory geometryFactory = new GeometryFactory();

  private static byte[] asWKB(Envelope value) {
    Geometry geometry = geometryFactory.toGeometry(value);
    return new WKBWriter(2, wkbNDR, true).write(geometry);
  }

  @Override
  protected void internalHandle(DataOutputStream buffer, Envelope value) throws Exception {
    byte[] wkb = asWKB(value);
    buffer.writeInt(wkb.length);
    buffer.write(wkb, 0, wkb.length);
  }

  @Override
  public int getLength(Envelope value) {
    return asWKB(value).length + 4;
  }
}
