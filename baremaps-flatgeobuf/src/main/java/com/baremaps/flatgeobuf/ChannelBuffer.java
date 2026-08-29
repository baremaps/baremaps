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

package com.baremaps.flatgeobuf;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;

/**
 * Sequential little endian reads over a channel, buffered so that a file made of many small records
 * costs few reads.
 * <p>
 * A channel hands back whatever it happens to have, which forces every caller to loop until it has
 * the bytes it asked for. This class does that looping once: the records of a FlatGeoBuf file have
 * a known length, so callers ask for a length and either get it or get an {@link EOFException}.
 */
class ChannelBuffer implements AutoCloseable {

  private final ReadableByteChannel channel;

  /**
   * Always held in read mode: the position is the next unread byte and the limit is the end of the
   * buffered data.
   */
  private ByteBuffer buffer;

  ChannelBuffer(ReadableByteChannel channel, int capacity) {
    this.channel = channel;
    this.buffer = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN).limit(0);
  }

  /**
   * Returns the next {@code length} bytes as a little endian buffer.
   * <p>
   * The result is a view over the internal buffer, so it is only valid until the next call. Callers
   * that need to keep the bytes must copy them, which is what {@link #readFully} is for.
   */
  ByteBuffer next(int length) throws IOException {
    if (buffer.remaining() < length) {
      fill(length);
    }
    ByteBuffer next = buffer.slice(buffer.position(), length).order(ByteOrder.LITTLE_ENDIAN);
    buffer.position(buffer.position() + length);
    return next;
  }

  /** Reads the next {@code length} bytes into a buffer of their own, ready to be read. */
  ByteBuffer readFully(int length) throws IOException {
    ByteBuffer destination = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
    int buffered = Math.min(buffer.remaining(), length);
    destination.put(buffer.slice(buffer.position(), buffered));
    buffer.position(buffer.position() + buffered);
    while (destination.hasRemaining()) {
      if (channel.read(destination) == -1) {
        throw new EOFException(
            "Expected %d bytes but the channel ended %d bytes short"
                .formatted(length, destination.remaining()));
      }
    }
    return destination.flip();
  }

  /** Discards the next {@code length} bytes, seeking over them when the channel allows it. */
  void skip(long length) throws IOException {
    long remaining = length;
    int buffered = (int) Math.min(buffer.remaining(), remaining);
    buffer.position(buffer.position() + buffered);
    remaining -= buffered;
    if (remaining == 0) {
      return;
    }
    if (channel instanceof SeekableByteChannel seekable) {
      if (seekable.position() + remaining > seekable.size()) {
        throw new EOFException(
            "Cannot skip %d bytes past the end of the channel".formatted(length));
      }
      seekable.position(seekable.position() + remaining);
      return;
    }
    while (remaining > 0) {
      ByteBuffer discarded = buffer.clear().limit((int) Math.min(buffer.capacity(), remaining));
      if (channel.read(discarded) == -1) {
        throw new EOFException(
            "Cannot skip %d bytes past the end of the channel".formatted(length));
      }
      remaining -= discarded.position();
    }
    buffer.limit(buffer.position());
  }

  /**
   * Makes at least {@code length} bytes available, growing the buffer for a record that is larger
   * than it is. The larger buffer is kept, on the assumption that a file with one oversized record
   * tends to hold others.
   */
  private void fill(int length) throws IOException {
    if (length > buffer.capacity()) {
      ByteBuffer grown = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
      grown.put(buffer);
      buffer = grown;
    } else {
      buffer.compact();
    }
    while (buffer.hasRemaining() && channel.read(buffer) != -1) {
      // Fill as much as the channel offers: a short read only matters if it leaves us short.
    }
    buffer.flip();
    if (buffer.remaining() < length) {
      throw new EOFException(
          "Expected %d bytes but the channel ended %d bytes short"
              .formatted(length, length - buffer.remaining()));
    }
  }

  @Override
  public void close() throws IOException {
    channel.close();
  }
}
