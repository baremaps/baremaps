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

package com.baremaps.iploc;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Utility methods for {@code InetAddress}. */
public class InetAddressUtils {

  private InetAddressUtils() {
    // Prevent instantiation
  }

  /**
   * Returns the {@code InetAddress} having the given address.
   *
   * @param address the byte array representation of the address
   * @return the {@code InetAddress}
   */
  public static InetAddress fromByteArray(byte[] address) {
    try {
      return InetAddress.getByAddress(address);
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Invalid address");
    }
  }
}
