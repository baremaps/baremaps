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

import com.baremaps.flatgeobuf.generated.Column;
import com.baremaps.flatgeobuf.generated.Crs;
import com.baremaps.flatgeobuf.generated.Feature;
import com.baremaps.flatgeobuf.generated.Header;
import com.google.flatbuffers.FlatBufferBuilder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import org.locationtech.jts.geom.Envelope;

/**
 * Writes a FlatGeoBuf file: its header, then its spatial index, then its features, in that order.
 * <p>
 * The FlatBuffers encoding stays inside this class, and so do the buffers it encodes into. They are
 * reused from one feature to the next, because a file has as many features as a dataset has rows
 * and an allocation per feature is an allocation too many.
 * <p>
 * This code has been adapted from FlatGeoBuf (BSD 2-Clause "Simplified" License).
 * <p>
 * Copyright (c) 2018, Bj&ouml;rn Harrtell
 */
public class FlatGeoBufWriter implements AutoCloseable {

  private static final int BUILDER_CAPACITY = 1 << 12;

  private static final int PROPERTIES_CAPACITY = 1 << 10;

  private final WritableByteChannel channel;

  private final FlatBufferBuilder builder = new FlatBufferBuilder(BUILDER_CAPACITY);

  private final ByteBuffer length =
      ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);

  private ByteBuffer properties =
      ByteBuffer.allocate(PROPERTIES_CAPACITY).order(ByteOrder.LITTLE_ENDIAN);

  private FlatGeoBuf.Header header;

  public FlatGeoBufWriter(WritableByteChannel channel) {
    this.channel = channel;
  }

  /** Writes the header, which the features are encoded against and which therefore comes first. */
  public void writeHeader(FlatGeoBuf.Header header) throws IOException {
    ByteBuffer encoded = encodeHeader(header);
    ByteBuffer prefix = ByteBuffer.allocate(FlatGeoBuf.MAGIC.length + Integer.BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put(FlatGeoBuf.MAGIC)
        .putInt(encoded.remaining())
        .flip();
    writeFully(prefix);
    writeFully(encoded);
    this.header = header;
  }

  /**
   * Writes the spatial index that follows the header. The index has to describe the features that
   * follow it, so in practice this takes the index of the file being copied or rewritten.
   */
  public void writeIndex(ByteBuffer index) throws IOException {
    writeFully(index);
  }

  /** Writes a feature, whose properties must line up with the columns of the header. */
  public void writeFeature(FlatGeoBuf.Feature feature) throws IOException {
    ByteBuffer encoded = encodeFeature(feature, requireHeader());
    writeFully(length.clear().putInt(encoded.remaining()).flip());
    writeFully(encoded);
  }

  @Override
  public void close() throws IOException {
    channel.close();
  }

  private ByteBuffer encodeHeader(FlatGeoBuf.Header header) {
    builder.clear();

    // FlatBuffers cannot nest a table inside another, so every string, vector and table the header
    // refers to has to be built before the header table is started.
    int[] columns = new int[header.columns().size()];
    for (int i = 0; i < columns.length; i++) {
      columns[i] = encodeColumn(header.columns().get(i));
    }
    int columnsOffset = Header.createColumnsVector(builder, columns);
    int envelopeOffset = encodeEnvelope(header.envelope());
    int crsOffset = encodeCrs(header.crs());
    int nameOffset = encodeString(header.name());
    int titleOffset = encodeString(header.title());
    int descriptionOffset = encodeString(header.description());
    int metadataOffset = encodeString(header.metadata());

    Header.startHeader(builder);
    Header.addName(builder, nameOffset);
    Header.addEnvelope(builder, envelopeOffset);
    Header.addGeometryType(builder, header.geometryType().value());
    Header.addHasZ(builder, header.hasZ());
    Header.addHasM(builder, header.hasM());
    Header.addHasT(builder, header.hasT());
    Header.addHasTm(builder, header.hasTm());
    Header.addColumns(builder, columnsOffset);
    Header.addFeaturesCount(builder, header.featuresCount());
    Header.addIndexNodeSize(builder, header.indexNodeSize());
    Header.addCrs(builder, crsOffset);
    Header.addTitle(builder, titleOffset);
    Header.addDescription(builder, descriptionOffset);
    Header.addMetadata(builder, metadataOffset);
    builder.finish(Header.endHeader(builder));

    return builder.dataBuffer().duplicate();
  }

  private int encodeColumn(FlatGeoBuf.Column column) {
    return Column.createColumn(
        builder,
        encodeString(column.name()),
        column.type().value(),
        encodeString(column.title()),
        encodeString(column.description()),
        column.width(),
        column.precision(),
        column.scale(),
        column.nullable(),
        column.unique(),
        column.primaryKey(),
        encodeString(column.metadata()));
  }

  private int encodeEnvelope(Envelope envelope) {
    if (envelope == null || envelope.isNull()) {
      return 0;
    }
    return Header.createEnvelopeVector(builder, new double[] {
        envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY()});
  }

  private int encodeCrs(FlatGeoBuf.Crs crs) {
    if (crs == null) {
      return 0;
    }
    int orgOffset = encodeString(crs.org());
    int nameOffset = encodeString(crs.name());
    int descriptionOffset = encodeString(crs.description());
    int wktOffset = encodeString(crs.wkt());
    int codeStringOffset = encodeString(crs.codeString());

    Crs.startCrs(builder);
    Crs.addOrg(builder, orgOffset);
    Crs.addCode(builder, crs.code());
    Crs.addName(builder, nameOffset);
    Crs.addDescription(builder, descriptionOffset);
    Crs.addWkt(builder, wktOffset);
    Crs.addCodeString(builder, codeStringOffset);
    return Crs.endCrs(builder);
  }

  /** Returns the offset of {@code value}, or 0 for the null that FlatBuffers reads as absent. */
  private int encodeString(String value) {
    return value == null ? 0 : builder.createString(value);
  }

  private ByteBuffer encodeFeature(FlatGeoBuf.Feature feature, FlatGeoBuf.Header header) {
    properties = PropertyCodec.encode(properties, feature.properties(), header.columns());

    builder.clear();
    int propertiesOffset =
        properties.hasRemaining() ? Feature.createPropertiesVector(builder, properties) : 0;
    int geometryOffset = feature.geometry() == null ? 0
        : GeometryConversions.write(builder, feature.geometry(), header.geometryType(),
            header.hasZ(), header.hasM());

    Feature.startFeature(builder);
    Feature.addGeometry(builder, geometryOffset);
    Feature.addProperties(builder, propertiesOffset);
    builder.finish(Feature.endFeature(builder));

    return builder.dataBuffer().duplicate();
  }

  private FlatGeoBuf.Header requireHeader() {
    if (header == null) {
      throw new IllegalStateException("The header has to be written before the rest of the file");
    }
    return header;
  }

  private void writeFully(ByteBuffer buffer) throws IOException {
    while (buffer.hasRemaining()) {
      channel.write(buffer);
    }
  }

}
