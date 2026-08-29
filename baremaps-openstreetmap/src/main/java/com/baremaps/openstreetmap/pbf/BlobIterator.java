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

import com.baremaps.data.stream.StreamException;
import com.baremaps.osm.binary.Fileformat;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** An iterator over the blobs of an OpenStreetMap PBF {@code InputStream}. */
class BlobIterator implements Iterator<Blob> {

  private final DataInputStream input;
  private Blob next;
  private boolean done;

  BlobIterator(InputStream input) {
    this.input = new DataInputStream(input);
  }

  /**
   * Reads the next blob, or returns null at the end of the stream. Only an end of stream that falls
   * exactly between two blobs is a normal termination; a truncated blob is an error.
   */
  private Blob read() throws IOException {
    int headerSize;
    try {
      headerSize = input.readInt();
    } catch (EOFException e) {
      return null;
    }
    byte[] headerBytes = new byte[headerSize];
    input.readFully(headerBytes);
    Fileformat.BlobHeader header = Fileformat.BlobHeader.parseFrom(headerBytes);
    byte[] data = new byte[header.getDatasize()];
    input.readFully(data);
    return new Blob(header.getType(), data);
  }

  @Override
  public boolean hasNext() {
    if (next == null && !done) {
      try {
        next = read();
      } catch (IOException e) {
        throw new StreamException(e);
      }
      done = next == null;
    }
    return next != null;
  }

  @Override
  public Blob next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    Blob current = next;
    next = null;
    return current;
  }
}
