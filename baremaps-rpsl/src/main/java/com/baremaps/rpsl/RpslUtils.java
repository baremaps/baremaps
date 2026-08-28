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

package com.baremaps.rpsl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility methods for RPSL objects.
 */
public class RpslUtils {

  private RpslUtils() {
    // Prevent instantiation
  }

  /**
   * Returns true if the RPSL object is an inetnum.
   *
   * @param object
   * @return
   */
  public static boolean isInetnum(RpslObject object) {
    return "inetnum".equals(object.type()) || "inet6num".equals(object.type());
  }

  /**
   * Writes a RPSL object into a text file.
   *
   * @param path the path of a text file
   */
  public static void writeToFile(Path path, RpslObject object) throws IOException {
    if (Files.notExists(path)) {
      Files.createDirectories(path);
    }
    var fileName = object.type() + "-" + object.id() + ".txt";
    var fileOutputStream = new FileOutputStream(path + "/" + fileName);
    try (var outStream = new DataOutputStream(new BufferedOutputStream(fileOutputStream))) {
      outStream.writeUTF(object.toString());
    }
  }
}
