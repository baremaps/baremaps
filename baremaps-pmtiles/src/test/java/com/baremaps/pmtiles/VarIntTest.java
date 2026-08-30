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

package com.baremaps.pmtiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Tests for the VarInt class.
 */
class VarIntTest {

  @Test
  void read() throws IOException {
    var input = new ByteArrayInputStream(new byte[] {
        (byte) 0, (byte) 1,
        (byte) 127, (byte) 0xe5,
        (byte) 0x8e, (byte) 0x26
    });
    assertEquals(0, VarInt.read(input));
    assertEquals(1, VarInt.read(input));
    assertEquals(127, VarInt.read(input));
    assertEquals(624485, VarInt.read(input));
  }

  @Test
  void readBeyondThirtyTwoBits() throws IOException {
    var input = new ByteArrayInputStream(new byte[] {
        (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0x0f,
    });
    assertEquals(9007199254740991L, VarInt.read(input));
  }

  @Test
  void writeAndRead() throws IOException {
    for (long value = 0; value < 1000; value++) {
      assertEquals(value, writeAndRead(value));
    }
    for (long value = Long.MAX_VALUE - 1000; value < Long.MAX_VALUE; value++) {
      assertEquals(value, writeAndRead(value));
    }
  }

  @Test
  void readTruncated() {
    // A stream that ends mid-value must fail rather than mistake the end for a continuation byte.
    var input = new ByteArrayInputStream(new byte[] {(byte) 0x80});
    assertThrows(EOFException.class, () -> VarInt.read(input));
  }

  @Test
  void readOverlong() {
    var bytes = new byte[11];
    java.util.Arrays.fill(bytes, (byte) 0x80);
    assertThrows(IOException.class, () -> VarInt.read(new ByteArrayInputStream(bytes)));
  }

  private static long writeAndRead(long value) throws IOException {
    var output = new ByteArrayOutputStream();
    VarInt.write(output, value);
    return VarInt.read(new ByteArrayInputStream(output.toByteArray()));
  }
}
