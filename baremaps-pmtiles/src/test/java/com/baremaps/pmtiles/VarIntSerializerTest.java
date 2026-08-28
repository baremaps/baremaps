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

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.io.LittleEndianDataInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Tests for the VarIntSerializer class.
 */
class VarIntSerializerTest {

  private final VarIntSerializer varIntSerializer = new VarIntSerializer();

  @Test
  void decodeVarInt() throws IOException {
    var b = new LittleEndianDataInputStream(new ByteArrayInputStream(new byte[] {
        (byte) 0, (byte) 1,
        (byte) 127, (byte) 0xe5,
        (byte) 0x8e, (byte) 0x26
    }));
    assertEquals(0, varIntSerializer.readVarInt(b));
    assertEquals(1, varIntSerializer.readVarInt(b));
    assertEquals(127, varIntSerializer.readVarInt(b));
    assertEquals(624485, varIntSerializer.readVarInt(b));
    b = new LittleEndianDataInputStream(new ByteArrayInputStream(new byte[] {
        (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0x0f,
    }));
    assertEquals(9007199254740991L, varIntSerializer.readVarInt(b));
  }

  @Test
  void encodeVarInt() throws IOException {
    for (long i = 0; i < 1000; i++) {
      var array = new ByteArrayOutputStream();
      varIntSerializer.writeVarInt(array, i);
      var input = new LittleEndianDataInputStream(new ByteArrayInputStream(array.toByteArray()));
      assertEquals(i, varIntSerializer.readVarInt(input));
    }
    for (long i = Long.MAX_VALUE - 1000; i < Long.MAX_VALUE; i++) {
      var array = new ByteArrayOutputStream();
      varIntSerializer.writeVarInt(array, i);
      var input = new LittleEndianDataInputStream(new ByteArrayInputStream(array.toByteArray()));
      assertEquals(i, varIntSerializer.readVarInt(input));
    }
  }
}
