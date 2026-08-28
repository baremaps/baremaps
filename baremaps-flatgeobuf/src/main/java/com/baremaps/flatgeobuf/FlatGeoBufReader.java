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
import com.baremaps.flatgeobuf.generated.Feature;
import com.baremaps.flatgeobuf.generated.Header;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.locationtech.jts.geom.Geometry;

/**
 * This class contains the logic for reading FlatGeoBuf files. It can either read FlatBuffers
 * directly or convert them to a domain model.
 * <p>
 * This code has been adapted from FlatGeoBuf (BSD 2-Clause "Simplified" License).
 * <p>
 * Copyright (c) 2018, Bj&ouml;rn Harrtell
 */
public class FlatGeoBufReader implements AutoCloseable {

  private final ByteBuffer featureBuffer =
      ByteBuffer.allocate(1 << 16).order(ByteOrder.LITTLE_ENDIAN);

  private final ReadableByteChannel channel;

  private Header header;

  public FlatGeoBufReader(ReadableByteChannel channel) {
    this.channel = channel;
  }

  public Header readHeaderBuffer() throws IOException {
    header = readHeaderBuffer(channel);
    return header;
  }

  public FlatGeoBuf.Header readHeader() throws IOException {
    return asFlatGeoBuf(readHeaderBuffer());
  }

  public Feature readFeatureBuffer() throws IOException {
    return readFeatureBuffer(channel, featureBuffer);
  }

  public FlatGeoBuf.Feature readFeature() throws IOException {
    return readFeature(channel, header, featureBuffer);
  }

  public void skipIndex() throws IOException {
    skipIndex(channel, header);
  }

  public ByteBuffer readIndexBuffer() throws IOException {
    return readIndexBuffer(channel, header);
  }

  public InputStream readIndexStream() {
    return readIndexStream(channel, header);
  }

  @Override
  public void close() throws IOException {
    channel.close();
  }

  public static Header readHeaderBuffer(ReadableByteChannel channel)
      throws IOException {

    // Check if the file is a flatgeobuf
    ByteBuffer prefixBuffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
    while (prefixBuffer.hasRemaining()) {
      if (channel.read(prefixBuffer) == -1) {
        break; // End of channel reached
      }
    }
    prefixBuffer.flip();
    if (!FlatGeoBuf.isFlatGeoBuf(prefixBuffer)) {
      throw new IOException("This is not a flatgeobuf!");
    }

    // Read the header size
    int headerSize = prefixBuffer.getInt();
    ByteBuffer headerBuffer = ByteBuffer.allocate(headerSize).order(ByteOrder.LITTLE_ENDIAN);

    // Read the header
    while (headerBuffer.hasRemaining()) {
      if (channel.read(headerBuffer) == -1) {
        break; // End of channel reached
      }
    }

    // Prepare the buffer for reading
    headerBuffer.flip();

    return Header.getRootAsHeader(headerBuffer);
  }

  public static FlatGeoBuf.Header readHeader(ReadableByteChannel channel)
      throws IOException {
    Header header = readHeaderBuffer(channel);
    return asFlatGeoBuf(header);
  }

  public static FlatGeoBuf.Header asFlatGeoBuf(Header header) {
    return new FlatGeoBuf.Header(
        header.name(),
        List.of(
            header.envelope(0),
            header.envelope(1),
            header.envelope(2),
            header.envelope(3)),
        FlatGeoBuf.GeometryType.values()[header.geometryType()],
        header.hasZ(),
        header.hasM(),
        header.hasT(),
        header.hasTm(),
        IntStream.range(0, header.columnsLength())
            .mapToObj(header::columns)
            .map(column -> new FlatGeoBuf.Column(
                column.name(),
                FlatGeoBuf.ColumnType.values()[column.type()],
                column.title(),
                column.description(),
                column.width(),
                column.precision(),
                column.scale(),
                column.nullable(),
                column.unique(),
                column.primaryKey(),
                column.metadata()))
            .toList(),
        header.featuresCount(),
        header.indexNodeSize(),
        new FlatGeoBuf.Crs(
            header.crs().org(),
            header.crs().code(),
            header.crs().name(),
            header.crs().description(),
            header.crs().wkt(),
            header.crs().codeString()),
        header.title(),
        header.description(),
        header.metadata());
  }

  public static Feature readFeatureBuffer(ReadableByteChannel channel, ByteBuffer buffer)
      throws IOException {

    try {
      // Compact the buffer if it has been used before
      if (buffer.position() > 0) {
        buffer.compact();
      }

      // Fill the buffer
      while (buffer.hasRemaining()) {
        if (channel.read(buffer) == -1) {
          break; // End of channel reached
        }
      }

      // Read the feature size
      buffer.flip();
      int featureSize = buffer.getInt();

      // Allocate a new buffer if the feature size is greater than the current buffer capacity
      if (featureSize > buffer.remaining()) {
        ByteBuffer newBuffer = ByteBuffer.allocate(featureSize).order(ByteOrder.LITTLE_ENDIAN);

        // Copy the remaining bytes from the current buffer to the new buffer
        newBuffer.put(buffer);

        // Fill the new buffer with the remaining bytes
        while (newBuffer.hasRemaining()) {
          if (channel.read(newBuffer) == -1) {
            break; // End of channel reached
          }
        }

        // Prepare the new buffer for reading
        newBuffer.flip();

        // Read the feature from the new buffer
        Feature feature = Feature.getRootAsFeature(newBuffer.duplicate());

        // Clear the old buffer to prepare for the next read
        buffer.clear();

        return feature;

      } else {
        Feature feature = Feature.getRootAsFeature(buffer.slice(buffer.position(), featureSize));
        buffer.position(buffer.position() + featureSize);

        return feature;
      }
    } catch (BufferUnderflowException e) {
      throw new IOException("Failed to read feature", e);
    }
  }

  public static FlatGeoBuf.Feature readFeature(
      ReadableByteChannel channel,
      Header header, ByteBuffer buffer)
      throws IOException {
    Feature feature = readFeatureBuffer(channel, buffer);
    return asFlatGeoBuf(header, feature);
  }

  public static FlatGeoBuf.Feature asFlatGeoBuf(Header header, Feature feature) {
    var properties = new ArrayList<>();
    if (feature.propertiesLength() > 0) {
      var propertiesBuffer = feature.propertiesAsByteBuffer();
      while (propertiesBuffer.hasRemaining()) {
        var columnPosition = propertiesBuffer.getShort();
        var columnType = header.columns(columnPosition);
        var columnValue = readValue(propertiesBuffer, columnType);
        properties.add(columnValue);
      }
    }
    Geometry geometry =
        GeometryConversions.readGeometry(feature.geometry(), header.geometryType());
    return new FlatGeoBuf.Feature(properties, geometry);
  }

  private static Object readValue(ByteBuffer buffer, Column column) {
    return switch (FlatGeoBuf.ColumnType.values()[column.type()]) {
      case BYTE -> buffer.get();
      case UBYTE -> buffer.get();
      case BOOL -> buffer.get() == 1;
      case SHORT -> buffer.getShort();
      case USHORT -> buffer.getShort();
      case INT -> buffer.getInt();
      case UINT -> buffer.getInt();
      case LONG -> buffer.getLong();
      case ULONG -> buffer.getLong();
      case FLOAT -> buffer.getFloat();
      case DOUBLE -> buffer.getDouble();
      case STRING -> readString(buffer);
      case JSON -> readJson(buffer);
      case DATETIME -> readDateTime(buffer);
      case BINARY -> readBinary(buffer);
    };
  }

  private static Object readString(ByteBuffer buffer) {
    var length = buffer.getInt();
    var bytes = new byte[length];
    buffer.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static Object readJson(ByteBuffer buffer) {
    throw new UnsupportedOperationException();
  }

  private static Object readDateTime(ByteBuffer buffer) {
    throw new UnsupportedOperationException();
  }

  private static Object readBinary(ByteBuffer buffer) {
    throw new UnsupportedOperationException();
  }

  private static class BoundedInputStream extends InputStream {
    private final InputStream in;
    private long remaining;

    private BoundedInputStream(InputStream in, long size) {
      this.in = in;
      this.remaining = size;
    }

    @Override
    public int read() throws IOException {
      if (remaining == 0) {
        return -1;
      }
      int result = in.read();
      if (result != -1) {
        remaining--;
      }
      return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (remaining == 0) {
        return -1;
      }
      int toRead = (int) Math.min(len, remaining);
      int result = in.read(b, off, toRead);
      if (result != -1) {
        remaining -= result;
      }
      return result;
    }

    @Override
    public long skip(long n) throws IOException {
      long toSkip = Math.min(n, remaining);
      long skipped = in.skip(toSkip);
      remaining -= skipped;
      return skipped;
    }

    @Override
    public int available() throws IOException {
      return (int) Math.min(in.available(), remaining);
    }

    @Override
    public void close() throws IOException {
      in.close();
    }
  }

  public static void skipIndex(ReadableByteChannel channel, Header header)
      throws IOException {
    long n = PackedRTree.calcSize(header.featuresCount(), header.indexNodeSize());

    // Define a buffer size for skipping bytes
    int bufferSize = 1 << 10;
    ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

    // Number of bytes left to skip
    long remaining = n;

    while (remaining > 0) {
      // Calculate how many bytes to read in this iteration
      int bytesToRead = (int) Math.min(bufferSize, remaining);

      // Set the buffer limit to the number of bytes to read
      buffer.limit(bytesToRead);

      // Read bytes into the buffer
      int bytesRead = channel.read(buffer);

      // Check if end of stream is reached
      if (bytesRead == -1) {
        throw new IOException("End of stream reached before skipping the required number of bytes");
      }

      // Update the remaining bytes to skip
      remaining -= bytesRead;

      // Clear the buffer for the next read
      buffer.clear();
    }
  }

  public static ByteBuffer readIndexBuffer(ReadableByteChannel channel, Header header)
      throws IOException {

    // Calculate the size of the index
    long indexSize = PackedRTree.calcSize(header.featuresCount(), header.indexNodeSize());
    if (indexSize > 1L << 31) {
      throw new IOException("Index size is greater than 2GB!");
    }

    // Read the index
    ByteBuffer buffer = ByteBuffer.allocate((int) indexSize).order(ByteOrder.LITTLE_ENDIAN);
    while (buffer.hasRemaining()) {
      if (channel.read(buffer) == -1) {
        break; // End of channel reached
      }
    }

    // Prepare the buffer for reading
    buffer.flip();
    return buffer;
  }

  public static InputStream readIndexStream(ReadableByteChannel channel, Header header) {
    long indexSize = PackedRTree.calcSize(header.featuresCount(), header.indexNodeSize());
    return new BoundedInputStream(Channels.newInputStream(channel), indexSize);
  }
}
