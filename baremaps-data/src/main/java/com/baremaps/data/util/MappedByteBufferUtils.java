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

package com.baremaps.data.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

/** Utilities for working with memory-mapped files. */
public class MappedByteBufferUtils {

  private MappedByteBufferUtils() {
    // Prevent instantiation
  }

  /**
   * Attempt to force-unmap a list of memory-mapped file segments, so it can safely be deleted.
   *
   * @param buffer The buffer to unmap
   */
  @SuppressWarnings("squid:S1141")
  public static void unmap(MappedByteBuffer buffer) {
    try {
      // attempt to force-unmap the file, so we can delete it later
      // https://stackoverflow.com/questions/2972986/how-to-unmap-a-file-from-memory-mapped-using-filechannel-in-java
      Class<?> unsafeClass;
      try {
        unsafeClass = Class.forName("sun.misc.Unsafe");
      } catch (Exception ex) {
        unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
      }
      Method clean = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
      clean.setAccessible(true);
      Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
      theUnsafeField.setAccessible(true);
      Object theUnsafe = theUnsafeField.get(null);
      if (buffer != null) {
        buffer.force();
        clean.invoke(theUnsafe, buffer);
      }
    } catch (Exception e) {
      // failed to unmap, but we can still delete the file
      // throw new IOException("Unable to unmap", e);
    }
  }
}
