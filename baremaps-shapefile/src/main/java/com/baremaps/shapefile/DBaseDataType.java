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

/**
 * Provides a simple DataType class.
 *
 * @author Travis L. Pinney
 * @see <a href="http://www.clicketyclick.dk/databases/xbase/format/data_types.html">Xbase Data
 *      Types</a>
 */
public enum DBaseDataType {
  /** Character (less than 254 characters). */
  CHARACTER('C'),

  /** Number (less than 18 characters, can include sign and decimal). */
  NUMBER('N'),

  /** Logical (3 way, ? Y,y,T,t N,n,F,f). */
  LOGICAL('L'),

  /** Date (YYYYMMDD format). */
  DATE('D'),

  /** Memo (Pointer to ASCII text field). */
  MEMO('M'),

  /** Floating point (20 digits). */
  FLOATING_POINT('F'),

  // CharacterNameVariable("?"), //1-254 Characters

  /** Picture (memo). */
  PICTURE('P'),

  /** Currency (Foxpro). */
  CURRENCY('Y'),

  /**
   * Date time (32 bit little-endian Julian date, 32 byte little endian milliseconds since
   * midnight).
   */
  DATE_TIME('T'),

  /** Integer (4 byte little endian). */
  INTEGER('I'),

  /** Varifield (???). */
  VARI_FIELD('V'),

  /** Variant (???). */
  VARIANT('X'),

  /** Time stamp (see url). */
  TIMESTAMP('@'),

  /** Double. */
  DOUBLE('O'),

  /** Auto increment. */
  AUTO_INCREMENT('+');

  /** Data type. */
  public final char datatype;

  /**
   * Construct a datatype.
   *
   * @param type Data type.
   */
  DBaseDataType(char type) {
    this.datatype = type;
  }

  /**
   * Return the Datatype enum of a code.
   *
   * @param code Character code describing the dbf datatype.
   * @return Datatype.
   */
  public static DBaseDataType valueOfDataType(char code) {
    for (DBaseDataType v : values()) {
      if (v.datatype == code) {
        return v;
      }
    }
    throw new IllegalArgumentException("Enum datatype is incorrect");
  }
}
