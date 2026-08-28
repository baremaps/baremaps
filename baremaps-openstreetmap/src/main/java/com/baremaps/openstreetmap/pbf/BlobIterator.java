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

package com.baremaps.openstreetmap.pbf;



import com.baremaps.openstreetmap.model.Blob;
import com.baremaps.osm.binary.Fileformat;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** An iterator over the blobs of an OpenStreetMap PBF {@code InputStream}. */
class BlobIterator implements Iterator<Blob> {

  private final DataInputStream dis;

  private Blob next;

  /**
   * Constructs a {@code BlobIterator} with the specified {@code InputStream}.
   *
   * @param input
   */
  public BlobIterator(InputStream input) {
    this.dis = new DataInputStream(input);
  }

  private Blob read() throws IOException {
    // Read blob header
    int headerSize = dis.readInt();
    byte[] headerBytes = new byte[headerSize];
    dis.readFully(headerBytes);
    Fileformat.BlobHeader header = Fileformat.BlobHeader.parseFrom(headerBytes);

    // Read blob data
    int dataSize = header.getDatasize();
    byte[] data = new byte[dataSize];
    dis.readFully(data);

    return new Blob(header, data, 8 + headerSize + dataSize);
  }

  /**
   * Returns true if the iteration has more blobs.
   *
   * @return true if the iteration has more elements
   */
  @Override
  public boolean hasNext() {
    try {
      if (next == null) {
        next = read();
      }
      return true;
    } catch (IOException exception) {
      return false;
    }
  }

  /**
   * Returns the next blob in the iteration.
   *
   * @return the next blob in the iteration
   */
  @Override
  public Blob next() {
    try {
      if (next == null) {
        next = read();
      }
      Blob current = next;
      next = null;
      return current;
    } catch (IOException exception) {
      throw new NoSuchElementException();
    }
  }
}
