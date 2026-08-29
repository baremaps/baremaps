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

package com.baremaps.flatgeobuf;

import com.baremaps.flatgeobuf.FlatGeoBuf.ColumnType;
import com.baremaps.flatgeobuf.FlatGeoBuf.GeometryType;
import com.baremaps.flatgeobuf.generated.Column;
import com.baremaps.flatgeobuf.generated.Crs;
import com.baremaps.flatgeobuf.generated.Feature;
import com.baremaps.flatgeobuf.generated.Header;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Reads a FlatGeoBuf file: its header, then its spatial index, then its features, in that order.
 * <p>
 * The FlatBuffers encoding stays inside this class. Callers see the records of {@link FlatGeoBuf},
 * which own their data and outlive any buffer the reader reuses.
 * <p>
 * This code has been adapted from FlatGeoBuf (BSD 2-Clause "Simplified" License).
 * <p>
 * Copyright (c) 2018, Bj&ouml;rn Harrtell
 */
public class FlatGeoBufReader implements AutoCloseable {

  /** Sized so that a file of small features costs one read per few hundred of them. */
  private static final int BUFFER_CAPACITY = 1 << 16;

  /**
   * A header is a few kilobytes at most, so a larger one means a corrupt or hostile file rather
   * than an allocation worth attempting.
   */
  private static final int MAX_HEADER_SIZE = 1 << 24;

  /** Four doubles for the bounding box and one long for the offset of what the node covers. */
  private static final int INDEX_NODE_SIZE = 4 * Double.BYTES + Long.BYTES;

  private final ChannelBuffer buffer;

  private final GeometryFactory geometryFactory;

  private FlatGeoBuf.Header header;

  public FlatGeoBufReader(ReadableByteChannel channel) {
    this(channel, new GeometryFactory());
  }

  /**
   * Creates a reader that builds its geometries with {@code geometryFactory}, which is how a caller
   * imposes an SRID, a precision model or a coordinate sequence implementation.
   */
  public FlatGeoBufReader(ReadableByteChannel channel, GeometryFactory geometryFactory) {
    this.buffer = new ChannelBuffer(channel, BUFFER_CAPACITY);
    this.geometryFactory = geometryFactory;
  }

  /** Reads the header, which every other method needs and which therefore comes first. */
  public FlatGeoBuf.Header readHeader() throws IOException {
    ByteBuffer prefix = buffer.next(FlatGeoBuf.MAGIC.length + Integer.BYTES);
    verifyMagic(prefix);
    int size = prefix.getInt(FlatGeoBuf.MAGIC.length);
    if (size < 0 || size > MAX_HEADER_SIZE) {
      throw new IOException("Header of %d bytes is out of bounds".formatted(size));
    }
    header = decodeHeader(Header.getRootAsHeader(buffer.next(size)));
    return header;
  }

  /** Reads the spatial index that follows the header, as the bytes needed to copy a file. */
  public ByteBuffer readIndex() throws IOException {
    long size = indexSize();
    if (size > Integer.MAX_VALUE) {
      throw new IOException("Index of %d bytes does not fit in memory".formatted(size));
    }
    return buffer.readFully((int) size);
  }

  /** Skips the spatial index, which a sequential scan of the features does not need. */
  public void skipIndex() throws IOException {
    buffer.skip(indexSize());
  }

  /** Reads the next feature. The header states how many of them the file holds. */
  public FlatGeoBuf.Feature readFeature() throws IOException {
    List<FlatGeoBuf.Column> columns = requireHeader().columns();
    int size = buffer.next(Integer.BYTES).getInt();
    if (size < 0) {
      throw new IOException("Feature of %d bytes is out of bounds".formatted(size));
    }
    Feature feature = Feature.getRootAsFeature(buffer.next(size));

    ByteBuffer properties = feature.propertiesLength() == 0
        ? ByteBuffer.allocate(0)
        : feature.propertiesAsByteBuffer();

    return new FlatGeoBuf.Feature(
        PropertyCodec.decode(properties, columns),
        feature.geometry() == null
            ? null
            : GeometryConversions.read(geometryFactory, feature.geometry(),
                header.geometryType()));
  }

  @Override
  public void close() throws IOException {
    buffer.close();
  }

  /**
   * The size in bytes of the packed R-tree that sits between the header and the features. The tree
   * is complete, so its size follows from the number of features and the branching factor; an index
   * node size of zero means the file carries no index at all.
   */
  private long indexSize() {
    FlatGeoBuf.Header header = requireHeader();
    long nodeSize = Math.min(header.indexNodeSize(), Short.MAX_VALUE * 2 + 1);
    if (header.featuresCount() == 0 || nodeSize == 0) {
      return 0;
    }
    if (nodeSize < 2) {
      throw new IllegalStateException("Index node size must be at least 2 but is " + nodeSize);
    }
    long nodes = header.featuresCount();
    long level = header.featuresCount();
    do {
      level = (level + nodeSize - 1) / nodeSize;
      nodes += level;
    } while (level != 1);
    return nodes * INDEX_NODE_SIZE;
  }

  private FlatGeoBuf.Header requireHeader() {
    if (header == null) {
      throw new IllegalStateException("The header has to be read before the rest of the file");
    }
    return header;
  }

  private static void verifyMagic(ByteBuffer prefix) throws IOException {
    for (int i = 0; i < FlatGeoBuf.MAGIC.length; i++) {
      if (prefix.get(i) != FlatGeoBuf.MAGIC[i]) {
        throw new IOException("This is not a FlatGeoBuf file of the supported specification");
      }
    }
  }

  private static FlatGeoBuf.Header decodeHeader(Header header) {
    return new FlatGeoBuf.Header(
        header.name(),
        decodeEnvelope(header),
        GeometryType.of(header.geometryType()),
        header.hasZ(),
        header.hasM(),
        header.hasT(),
        header.hasTm(),
        decodeColumns(header),
        header.featuresCount(),
        header.indexNodeSize(),
        decodeCrs(header.crs()),
        header.title(),
        header.description(),
        header.metadata());
  }

  private static Envelope decodeEnvelope(Header header) {
    // An absent vector reads as zeroes rather than failing, so the length has to be checked.
    if (header.envelopeLength() < 4) {
      return null;
    }
    return new Envelope(
        header.envelope(0), header.envelope(2),
        header.envelope(1), header.envelope(3));
  }

  private static FlatGeoBuf.Crs decodeCrs(Crs crs) {
    if (crs == null) {
      return null;
    }
    return new FlatGeoBuf.Crs(
        crs.org(), crs.code(), crs.name(), crs.description(), crs.wkt(), crs.codeString());
  }

  private static List<FlatGeoBuf.Column> decodeColumns(Header header) {
    List<FlatGeoBuf.Column> columns = new ArrayList<>(header.columnsLength());
    for (int i = 0; i < header.columnsLength(); i++) {
      Column column = header.columns(i);
      columns.add(new FlatGeoBuf.Column(
          column.name(),
          ColumnType.of(column.type()),
          column.title(),
          column.description(),
          column.width(),
          column.precision(),
          column.scale(),
          column.nullable(),
          column.unique(),
          column.primaryKey(),
          column.metadata()));
    }
    return List.copyOf(columns);
  }
}
