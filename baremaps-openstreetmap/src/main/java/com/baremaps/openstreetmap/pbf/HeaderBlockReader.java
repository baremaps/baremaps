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

package com.baremaps.openstreetmap.pbf;

import com.baremaps.openstreetmap.model.Bound;
import com.baremaps.openstreetmap.model.Header;
import com.baremaps.openstreetmap.model.HeaderBlock;
import com.baremaps.osm.binary.Osmformat;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.zip.DataFormatException;

/** Decodes the header blob of an OpenStreetMap PBF file. */
final class HeaderBlockReader {

  /** Header bounding boxes are stored in nanodegrees. */
  private static final double NANODEGREE = 1e-9;

  private HeaderBlockReader() {}

  static HeaderBlock read(Blob blob) throws DataFormatException, InvalidProtocolBufferException {
    Osmformat.HeaderBlock headerBlock = Osmformat.HeaderBlock.parseFrom(blob.data());
    Header header = new Header(
        headerBlock.getOsmosisReplicationSequenceNumber(),
        LocalDateTime.ofEpochSecond(headerBlock.getOsmosisReplicationTimestamp(), 0,
            ZoneOffset.UTC),
        headerBlock.getOsmosisReplicationBaseUrl(),
        headerBlock.getSource(),
        headerBlock.getWritingprogram());
    Osmformat.HeaderBBox bbox = headerBlock.getBbox();
    Bound bound = new Bound(
        bbox.getTop() * NANODEGREE,
        bbox.getRight() * NANODEGREE,
        bbox.getBottom() * NANODEGREE,
        bbox.getLeft() * NANODEGREE);
    return new HeaderBlock(header, bound);
  }
}
