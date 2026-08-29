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

package com.baremaps.geoparquet;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.locationtech.jts.geom.Envelope;

/**
 * A {@link Spliterator} that walks a list of Parquet files, reading one at a time with a
 * {@link GeoParquetFileReader}. Splitting hands the files that have not been opened yet to another
 * spliterator, which is what makes a parallel read possible.
 * <p>
 * Readers are registered with the set the stream was created with, and removed from it once
 * exhausted, so that closing a stream that was only partly consumed still releases the file it had
 * open.
 */
class GeoParquetSpliterator implements Spliterator<GeoParquetGroup> {

  private final List<FileStatus> files;
  private final Configuration configuration;
  private final Envelope envelope;
  private final Set<GeoParquetFileReader> openReaders;

  private int index;
  private int endIndex;
  private GeoParquetFileReader reader;

  /**
   * Constructs a new {@code GeoParquetSpliterator} over a range of files. No file is opened until
   * the first record is requested, so that splitting stays cheap and free of side effects.
   *
   * @param files the files
   * @param envelope the envelope to filter the records, which may be null
   * @param configuration the configuration
   * @param index the index of the first file of the range
   * @param endIndex the index after the last file of the range
   * @param openReaders the readers the enclosing stream has open
   */
  GeoParquetSpliterator(
      List<FileStatus> files,
      Envelope envelope,
      Configuration configuration,
      int index,
      int endIndex,
      Set<GeoParquetFileReader> openReaders) {
    this.files = files;
    this.envelope = envelope;
    this.configuration = configuration;
    this.index = index;
    this.endIndex = endIndex;
    this.openReaders = openReaders;
  }

  @Override
  public boolean tryAdvance(Consumer<? super GeoParquetGroup> action) {
    try {
      while (reader != null || openNextFile()) {
        GeoParquetGroup group = reader.read();
        if (group != null) {
          action.accept(group);
          return true;
        }
        closeReader();
      }
      return false;
    } catch (IOException e) {
      closeReader();
      throw new GeoParquetException("Failed to read the next record.", e);
    }
  }

  private boolean openNextFile() throws IOException {
    while (index < endIndex) {
      reader = GeoParquetFileReader.open(files.get(index++), configuration, envelope);
      if (reader != null) {
        openReaders.add(reader);
        return true;
      }
    }
    return false;
  }

  private void closeReader() {
    if (reader != null) {
      openReaders.remove(reader);
      try {
        reader.close();
      } catch (IOException e) {
        throw new GeoParquetException("Failed to close a Parquet file reader.", e);
      } finally {
        reader = null;
      }
    }
  }

  @Override
  public Spliterator<GeoParquetGroup> trySplit() {
    int remainingFiles = endIndex - index;
    if (remainingFiles <= 1) {
      return null;
    }
    int mid = index + remainingFiles / 2;
    GeoParquetSpliterator split =
        new GeoParquetSpliterator(files, envelope, configuration, mid, endIndex, openReaders);
    this.endIndex = mid;
    return split;
  }

  @Override
  public long estimateSize() {
    // The records of a file are only counted as they are read, and the envelope rejects an unknown
    // number of them, so the size is genuinely unknown.
    return Long.MAX_VALUE;
  }

  @Override
  public int characteristics() {
    return NONNULL | IMMUTABLE;
  }
}
