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

import com.baremaps.flatgeobuf.FlatGeoBuf.Column;
import com.baremaps.flatgeobuf.FlatGeoBuf.ColumnType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The encoding of a feature's properties: a packed sequence of (column index, value) pairs, in
 * which an absent property is simply missing rather than encoded as null.
 * <p>
 * Both directions live here so that they cannot drift apart. They did: the column types the reader
 * accepted and the ones the writer produced were listed separately, and the writer was missing
 * three of them.
 */
class PropertyCodec {

  private PropertyCodec() {
    // Prevent instantiation
  }

  /**
   * Decodes the properties of one feature into a list holding one entry per column, in column
   * order, with null for the columns the feature does not carry.
   */
  static List<Object> decode(ByteBuffer buffer, List<Column> columns) {
    Object[] values = new Object[columns.size()];
    while (buffer.hasRemaining()) {
      int index = Short.toUnsignedInt(buffer.getShort());
      if (index >= values.length) {
        throw new IllegalArgumentException(
            "Property refers to column %d of a header that declares %d"
                .formatted(index, values.length));
      }
      values[index] = decodeValue(buffer, columns.get(index).type());
    }
    return Collections.unmodifiableList(Arrays.asList(values));
  }

  /**
   * Encodes the properties of one feature, which must hold one entry per column, in column order.
   * <p>
   * The values are written into {@code buffer} and the buffer actually used is returned, ready to
   * be read: a larger one is allocated when they do not fit. Callers are expected to keep it for
   * their next feature, so that a file of many features costs one allocation rather than one per
   * feature.
   */
  static ByteBuffer encode(ByteBuffer buffer, List<Object> values, List<Column> columns) {
    if (values.size() != columns.size()) {
      throw new IllegalArgumentException(
          "Feature has %d properties but the header declares %d columns"
              .formatted(values.size(), columns.size()));
    }
    ByteBuffer target = buffer.clear();
    for (int index = 0; index < values.size(); index++) {
      Object value = values.get(index);
      if (value == null) {
        continue;
      }
      ColumnType type = columns.get(index).type();
      byte[] bytes = variableLengthBytes(type, value);
      int length = Short.BYTES
          + (bytes == null ? fixedLength(type) : Integer.BYTES + bytes.length);
      if (target.remaining() < length) {
        target = grow(target, length);
      }
      target.putShort((short) index);
      if (bytes == null) {
        encodeFixed(target, type, value);
      } else {
        target.putInt(bytes.length);
        target.put(bytes);
      }
    }
    return target.flip();
  }

  private static ByteBuffer grow(ByteBuffer buffer, int required) {
    int capacity = Math.max(buffer.capacity() * 2, buffer.position() + required);
    return ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN).put(buffer.flip());
  }

  private static Object decodeValue(ByteBuffer buffer, ColumnType type) {
    return switch (type) {
      case BYTE, UBYTE -> buffer.get();
      case BOOL -> buffer.get() == 1;
      case SHORT, USHORT -> buffer.getShort();
      case INT, UINT -> buffer.getInt();
      case LONG, ULONG -> buffer.getLong();
      case FLOAT -> buffer.getFloat();
      case DOUBLE -> buffer.getDouble();
      case STRING, JSON, DATETIME -> new String(decodeBytes(buffer), StandardCharsets.UTF_8);
      case BINARY -> decodeBytes(buffer);
    };
  }

  private static byte[] decodeBytes(ByteBuffer buffer) {
    byte[] bytes = new byte[buffer.getInt()];
    buffer.get(bytes);
    return bytes;
  }

  /**
   * Returns the bytes of a length prefixed value, or null for a value of fixed length. Strings,
   * JSON and datetimes are all encoded as length prefixed UTF-8 by the specification.
   */
  private static byte[] variableLengthBytes(ColumnType type, Object value) {
    return switch (type) {
      case STRING, JSON, DATETIME -> ((String) value).getBytes(StandardCharsets.UTF_8);
      case BINARY -> (byte[]) value;
      default -> null;
    };
  }

  private static int fixedLength(ColumnType type) {
    return switch (type) {
      case BYTE, UBYTE, BOOL -> Byte.BYTES;
      case SHORT, USHORT -> Short.BYTES;
      case INT, UINT, FLOAT -> Integer.BYTES;
      case LONG, ULONG, DOUBLE -> Long.BYTES;
      default -> throw new IllegalArgumentException("Not a fixed length column type: " + type);
    };
  }

  private static void encodeFixed(ByteBuffer buffer, ColumnType type, Object value) {
    switch (type) {
      case BYTE, UBYTE -> buffer.put((Byte) value);
      case BOOL -> buffer.put((byte) ((Boolean) value ? 1 : 0));
      case SHORT, USHORT -> buffer.putShort((Short) value);
      case INT, UINT -> buffer.putInt((Integer) value);
      case LONG, ULONG -> buffer.putLong((Long) value);
      case FLOAT -> buffer.putFloat((Float) value);
      case DOUBLE -> buffer.putDouble((Double) value);
      default -> throw new IllegalArgumentException("Not a fixed length column type: " + type);
    }
  }
}
