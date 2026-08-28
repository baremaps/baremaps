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



import com.baremaps.openstreetmap.model.Blob;
import com.baremaps.openstreetmap.model.Bound;
import com.baremaps.openstreetmap.model.Header;
import com.baremaps.openstreetmap.model.HeaderBlock;
import com.baremaps.openstreetmap.stream.StreamException;
import com.baremaps.osm.binary.Osmformat;
import com.baremaps.osm.binary.Osmformat.HeaderBBox;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.zip.DataFormatException;

/** A reader that extracts header blocks and entities from OpenStreetMap header blobs. */
public class HeaderBlockReader {

  public static final DateTimeFormatter format =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  private final Blob blob;

  private final Osmformat.HeaderBlock headerBlock;

  /**
   * Constructs a reader with the specified blob.
   *
   * @param blob the blob
   * @throws DataFormatException
   * @throws InvalidProtocolBufferException
   */
  public HeaderBlockReader(Blob blob) throws DataFormatException, InvalidProtocolBufferException {
    this.blob = blob;
    this.headerBlock = Osmformat.HeaderBlock.parseFrom(blob.data());
  }

  /**
   * Returns the {@code HeaderBlock}.
   *
   * @return the header block
   */
  public HeaderBlock read() {
    LocalDateTime timestamp = LocalDateTime
        .ofEpochSecond(headerBlock.getOsmosisReplicationTimestamp(), 0, ZoneOffset.UTC);
    Long replicationSequenceNumber = headerBlock.getOsmosisReplicationSequenceNumber();
    String replicationBaseUrl = headerBlock.getOsmosisReplicationBaseUrl();
    String source = headerBlock.getSource();
    String writingProgram = headerBlock.getWritingprogram();
    Header header = new Header(replicationSequenceNumber, timestamp, replicationBaseUrl, source,
        writingProgram);

    HeaderBBox headerBBox = headerBlock.getBbox();
    double minLon = headerBBox.getLeft() * .000000001;
    double maxLon = headerBBox.getRight() * .000000001;
    double minLat = headerBBox.getBottom() * .000000001;
    double maxLat = headerBBox.getTop() * .000000001;
    Bound bound = new Bound(maxLat, maxLon, minLat, minLon);

    return new HeaderBlock(blob, header, bound);
  }

  /**
   * Reads the provided header {@code Blob} and returns the corresponding {@code HeaderBlock}.
   *
   * @param blob the header blob
   * @return the header block
   */
  public static HeaderBlock read(Blob blob) {
    try {
      return new HeaderBlockReader(blob).read();
    } catch (DataFormatException | InvalidProtocolBufferException e) {
      throw new StreamException(e);
    }
  }
}
