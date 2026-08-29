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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.BlockMetaData;
import org.apache.parquet.hadoop.metadata.FileMetaData;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.schema.MessageType;
import org.locationtech.jts.geom.Envelope;

/**
 * Reads the GeoParquet files matching a path with the stream API. The path may be a glob, in which
 * case the files are read one after the other, or in parallel. The Parquet schema and the
 * GeoParquet metadata are inferred from the files themselves, and records may be filtered by
 * envelope.
 * <p>
 * The reader holds no file open; the streams it returns do. A stream that is not consumed to the
 * end must therefore be closed, which is what a try-with-resources block around it does.
 */
public class GeoParquetReader {

  private final Configuration configuration;
  private final List<FileStatus> files;
  private final Envelope envelope;

  /** The footer of the first file, read at most once as it is what the schema accessors return. */
  private FileInfo firstFile;

  /** The number of records of all the files, summed at most once. */
  private Long recordCount;

  /**
   * Constructs a new {@code GeoParquetReader}.
   *
   * @param path the path to read from, which may be a glob
   */
  public GeoParquetReader(Path path) {
    this(path, null, new Configuration());
  }

  /**
   * Constructs a new {@code GeoParquetReader}.
   *
   * @param path the path to read from, which may be a glob
   * @param envelope the envelope to filter the records, which may be null
   */
  public GeoParquetReader(Path path, Envelope envelope) {
    this(path, envelope, new Configuration());
  }

  /**
   * Constructs a new {@code GeoParquetReader}.
   *
   * @param path the path to read from, which may be a glob
   * @param envelope the envelope to filter the records, which may be null
   * @param configuration the configuration of the file system to read from
   */
  public GeoParquetReader(Path path, Envelope envelope, Configuration configuration) {
    this.configuration = configuration;
    this.files = listFiles(path, configuration);
    this.envelope = envelope;
  }

  /**
   * Returns the Parquet schema of the first file.
   *
   * @return the Parquet schema
   */
  public MessageType getParquetSchema() {
    return firstFile().schema();
  }

  /**
   * Returns the GeoParquet metadata of the first file.
   *
   * @return the metadata, or null when the file declares none
   */
  public GeoParquetMetadata getGeoParquetMetadata() {
    return firstFile().metadata();
  }

  /**
   * Returns the GeoParquet schema of the first file.
   *
   * @return the GeoParquet schema
   */
  public GeoParquetSchema getGeoParquetSchema() {
    return firstFile().geoParquetSchema();
  }

  /**
   * Tells whether all the files share one and the same Parquet schema. Reading a glob whose files
   * disagree yields records that cannot be read the same way, which the schema accessors, which
   * only look at the first file, would not reveal.
   *
   * @return true if all the files have the same schema
   */
  public boolean validateSchemasAreIdentical() {
    return files.parallelStream().map(this::readFileInfo).map(FileInfo::schema).distinct()
        .count() == 1;
  }

  /**
   * Returns the number of records of all the files, read from their footers rather than by reading
   * the records themselves. The envelope is not taken into account.
   *
   * @return the number of records
   */
  public synchronized long size() {
    if (recordCount == null) {
      recordCount = files.parallelStream()
          .map(this::readFileInfo)
          .mapToLong(FileInfo::recordCount)
          .sum();
    }
    return recordCount;
  }

  /**
   * Returns a stream over the records of all the files. The stream holds the file it is reading
   * open, and must be closed unless it is consumed to the end.
   *
   * @return a stream of groups
   */
  public Stream<GeoParquetGroup> read() {
    return stream(false);
  }

  /**
   * Returns a parallel stream over the records of all the files, which are split file by file. The
   * stream holds the files it is reading open, and must be closed unless it is consumed to the end.
   *
   * @return a parallel stream of groups
   */
  public Stream<GeoParquetGroup> readParallel() {
    return stream(true);
  }

  private Stream<GeoParquetGroup> stream(boolean parallel) {
    Set<GeoParquetFileReader> openReaders = ConcurrentHashMap.newKeySet();
    Spliterator<GeoParquetGroup> spliterator =
        new GeoParquetSpliterator(files, envelope, configuration, 0, files.size(), openReaders);
    return StreamSupport.stream(spliterator, parallel).onClose(() -> closeAll(openReaders));
  }

  private static void closeAll(Set<GeoParquetFileReader> openReaders) {
    for (GeoParquetFileReader reader : openReaders) {
      try {
        reader.close();
      } catch (IOException e) {
        throw new GeoParquetException("Failed to close a Parquet file reader.", e);
      }
    }
    openReaders.clear();
  }

  private synchronized FileInfo firstFile() {
    if (firstFile == null) {
      firstFile = files.stream()
          .findFirst()
          .map(this::readFileInfo)
          .orElseThrow(() -> new GeoParquetException("No Parquet file found at the given path."));
    }
    return firstFile;
  }

  private FileInfo readFileInfo(FileStatus file) {
    try {
      ParquetMetadata parquetMetadata =
          ParquetFileReader.readFooter(configuration, file.getPath());
      FileMetaData fileMetaData = parquetMetadata.getFileMetaData();
      MessageType schema = fileMetaData.getSchema();
      GeoParquetMetadata metadata = GeoParquetMetadata.read(fileMetaData.getKeyValueMetaData());
      long recordCount = parquetMetadata.getBlocks().stream()
          .mapToLong(BlockMetaData::getRowCount)
          .sum();
      return new FileInfo(recordCount, schema, metadata, GeoParquetSchema.of(schema, metadata));
    } catch (IOException e) {
      throw new GeoParquetException("Failed to read the footer of " + file.getPath(), e);
    }
  }

  private static List<FileStatus> listFiles(Path path, Configuration configuration) {
    try {
      FileSystem fileSystem = FileSystem.get(path.toUri(), configuration);
      FileStatus[] files = fileSystem.globStatus(path);
      if (files == null) {
        throw new GeoParquetException("No file found at " + path);
      }
      return Collections.unmodifiableList(Arrays.asList(files));
    } catch (IOException e) {
      throw new GeoParquetException("Failed to list the files at " + path, e);
    }
  }

  private record FileInfo(
      long recordCount,
      MessageType schema,
      GeoParquetMetadata metadata,
      GeoParquetSchema geoParquetSchema) {
  }
}
