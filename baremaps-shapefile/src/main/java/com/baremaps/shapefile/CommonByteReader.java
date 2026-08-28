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



import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

/**
 * Common byte reader.
 *
 * @author Marc Le Bihan
 */
public abstract class CommonByteReader implements AutoCloseable {

  /** The File. */
  private final File file;

  /** Input Stream on the DBF. */
  private final FileInputStream fis;

  /** File channel on the file. */
  private final FileChannel fc;

  /** Buffer reader. */
  private final MappedByteBuffer byteBuffer;

  /** Indicates if the byte buffer is closed. */
  private boolean isClosed = false;

  /**
   * Create and open a byte reader based on a file.
   *
   * @param f File.
   */
  protected CommonByteReader(File f) throws IOException {
    Objects.requireNonNull(f, "The file cannot be null.");
    this.file = f;
    this.fis = new FileInputStream(this.file);
    this.fc = this.fis.getChannel();
    this.byteBuffer = this.fc.map(FileChannel.MapMode.READ_ONLY, 0, this.fc.size());
  }

  /**
   * Close the MappedByteReader.
   *
   * @throws IOException if the close operation fails.
   */
  @Override
  public void close() throws IOException {
    if (this.fc != null) {
      this.fc.close();
    }
    if (this.fis != null) {
      this.fis.close();
    }
    this.isClosed = true;
  }

  /**
   * Returns the closed state of this binary reader.
   *
   * @return true if it is closed.
   */
  public boolean isClosed() {
    return this.isClosed;
  }

  /**
   * Returns the byte buffer.
   *
   * @return Byte Buffer.
   */
  public MappedByteBuffer getByteBuffer() {
    return this.byteBuffer;
  }

  /**
   * Return the file mapped.
   *
   * @return File.
   */
  public File getFile() {
    return this.file;
  }
}
