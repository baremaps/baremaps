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

package com.baremaps.shapefile;



import java.nio.MappedByteBuffer;

/**
 * Field descriptor.
 *
 * @author Travis L. Pinney
 */
public class DBaseFieldDescriptor {
  /** Field name. */
  private byte[] fieldName = new byte[11];

  /** Field name as String, for performance issues. */
  private String stringFieldName;

  /** Field type. */
  private DBaseDataType fieldType;

  /** Field address (Field data address (address is set in memory; not useful on disk). */
  private byte[] fieldAddress = new byte[4];

  /** Field length. */
  private byte fieldLength;

  /** Decimal count. */
  private byte fieldDecimalCount;

  /** Reserved 2. */
  private byte[] dbasePlusLanReserved2 = new byte[2];

  /** Work area id. */
  @SuppressWarnings("unused") // Kept in case of later ALTER TABLE sql calls.
  private byte workAreaID;

  /** Reserved 3. */
  private byte[] dbasePlusLanReserved3 = new byte[2];

  /** Undocumented use. */
  @SuppressWarnings("unused") // Kept in case of later ALTER TABLE sql calls.
  private byte setFields;

  /**
   * Create a field descriptor from the current position of the binary stream.
   *
   * @param byteBuffer ByteBuffer.
   */
  public DBaseFieldDescriptor(MappedByteBuffer byteBuffer) {
    // Field name.
    byteBuffer.get(this.fieldName);

    // Field type.
    char dt = (char) byteBuffer.get();
    this.fieldType = DBaseDataType.valueOfDataType(dt);

    // Field address.
    byteBuffer.get(this.fieldAddress);

    // Length and scale.
    this.fieldLength = byteBuffer.get();
    this.fieldDecimalCount = byteBuffer.get();

    byteBuffer.getShort(); // reserved

    byteBuffer.get(this.dbasePlusLanReserved2);

    // Work area id.
    this.workAreaID = byteBuffer.get();

    byteBuffer.get(this.dbasePlusLanReserved3);

    // Fields.
    this.setFields = byteBuffer.get();

    byte[] data = new byte[6];
    byteBuffer.get(data); // reserved
  }

  /**
   * Returns the decimal count of that field.
   *
   * @return Decimal count.
   */
  public int getDecimalCount() {
    return Byte.toUnsignedInt(this.fieldDecimalCount);
  }

  /**
   * Returns the field length.
   *
   * @return field length.
   */
  public int getLength() {
    return Byte.toUnsignedInt(this.fieldLength);
  }

  /**
   * Return the field name.
   *
   * @return Field name.
   */
  public String getName() {
    // Converting bytes to String takes time. Only do that once.
    if (this.stringFieldName == null) {
      int length = this.fieldName.length;
      while (length != 0 && Byte.toUnsignedInt(this.fieldName[length - 1]) <= ' ') {
        length--;
      }

      this.stringFieldName = new String(this.fieldName, 0, length);
    }

    return this.stringFieldName;
  }

  /**
   * Return the field data type.
   *
   * @return Data type.
   */
  public DBaseDataType getType() {
    return (this.fieldType);
  }
}
