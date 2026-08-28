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

package com.baremaps.flatgeobuf.generated;

@SuppressWarnings("all")
public final class ColumnType {
  private ColumnType() {}

  public static final int Byte = 0;
  public static final int UByte = 1;
  public static final int Bool = 2;
  public static final int Short = 3;
  public static final int UShort = 4;
  public static final int Int = 5;
  public static final int UInt = 6;
  public static final int Long = 7;
  public static final int ULong = 8;
  public static final int Float = 9;
  public static final int Double = 10;
  public static final int String = 11;
  public static final int Json = 12;
  public static final int DateTime = 13;
  public static final int Binary = 14;

  public static final String[] names = {"Byte", "UByte", "Bool", "Short", "UShort", "Int", "UInt",
      "Long", "ULong", "Float", "Double", "String", "Json", "DateTime", "Binary",};

  public static String name(int e) {
    return names[e];
  }
}
