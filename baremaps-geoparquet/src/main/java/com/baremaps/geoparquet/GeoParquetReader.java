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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
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
 * This reader enables reading of GeoParquet files from a specified URI with the stream API. The
 * schema of the Parquet files and the corresponding geoparquet schema and metadata are
 * automatically inferred from the files. The reader can be used to read the records in a sequential
 * or parallel manner. It is also capable of filtering records based on an envelope.
 */
public class GeoParquetReader implements Closeable {

  protected final Configuration configuration;
  protected final List<FileStatus> files;
  private final AtomicLong groupCount = new AtomicLong(-1);
  private final Envelope envelope;

  /**
   * Constructs a new {@code GeoParquetReader}.
   *
   * @param path the path to read from
   */
  public GeoParquetReader(Path path) {
    this(path, null, new Configuration());
  }

  /**
   * Constructs a new {@code GeoParquetReader}.
   *
   * @param path the path to read from
   * @param envelope the envelope to filter records
   */
  public GeoParquetReader(Path path, Envelope envelope) {
    this(path, envelope, new Configuration());
  }

  /**
   * Constructs a new {@code GeoParquetReader}.
   *
   * @param path the path to read from
   * @param configuration the configuration
   */
  public GeoParquetReader(Path path, Envelope envelope, Configuration configuration) {
    this.configuration = configuration;
    this.files = initializeFiles(path, configuration);
    this.envelope = envelope;
  }

  public MessageType getParquetSchema() {
    return files.stream()
        .findFirst()
        .map(this::getFileInfo)
        .orElseThrow(
            () -> new GeoParquetException("No files available to read schema.")).messageType;
  }



  public GeoParquetMetadata getGeoParquetMetadata() {
    return files.stream()
        .findFirst()
        .map(this::getFileInfo)
        .orElseThrow(this::noParquetFilesAvailable)
        .metadata();
  }

  public GeoParquetSchema getGeoParquetSchema() {
    return files.stream()
        .findFirst()
        .map(this::getFileInfo)
        .orElseThrow(this::noParquetFilesAvailable)
        .geoParquetSchema();
  }

  public GeoParquetException noParquetFilesAvailable() {
    return new GeoParquetException("No parquet files available.");
  }

  public boolean validateSchemasAreIdentical() {
    // Verify that all files have the same schema
    Set<MessageType> schemas = files.parallelStream()
        .map(this::getFileInfo)
        .map(fileInfo -> fileInfo.messageType)
        .collect(Collectors.toSet());
    return schemas.size() == 1;
  }

  public long size() {
    if (groupCount.get() == -1) {
      long totalCount = files.parallelStream()
          .map(this::getFileInfo)
          .mapToLong(fileInfo -> fileInfo.recordCount)
          .sum();
      groupCount.set(totalCount);
    }
    return groupCount.get();
  }

  private FileInfo getFileInfo(FileStatus fileStatus) {
    try {
      ParquetMetadata parquetMetadata =
          ParquetFileReader.readFooter(configuration, fileStatus.getPath());

      long recordCount = parquetMetadata.getBlocks().stream()
          .mapToLong(BlockMetaData::getRowCount)
          .sum();

      FileMetaData fileMetaData = parquetMetadata.getFileMetaData();
      Map<String, String> keyValueMetadata = fileMetaData.getKeyValueMetaData();
      MessageType messageType = fileMetaData.getSchema();

      GeoParquetMetadata geoParquetMetadata = null;
      GeoParquetSchema geoParquetSchema = null;
      if (keyValueMetadata.containsKey("geo")) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        geoParquetMetadata =
            objectMapper.readValue(keyValueMetadata.get("geo"), GeoParquetMetadata.class);
        geoParquetSchema =
            GeoParquetGroupFactory.createGeoParquetSchema(messageType, geoParquetMetadata);
      }

      return new FileInfo(
          fileStatus,
          recordCount,
          keyValueMetadata,
          messageType,
          geoParquetMetadata,
          geoParquetSchema);

    } catch (IOException e) {
      throw new GeoParquetException("Failed to build FileInfo for file: " + fileStatus, e);
    }
  }

  private static List<FileStatus> initializeFiles(Path path, Configuration configuration) {
    try {
      FileSystem fileSystem = FileSystem.get(path.toUri(), configuration);
      FileStatus[] fileStatuses = fileSystem.globStatus(path);
      if (fileStatuses == null) {
        throw new GeoParquetException("No files found at the specified URI.");
      }
      return Collections.unmodifiableList(Arrays.asList(fileStatuses));
    } catch (IOException e) {
      throw new GeoParquetException("IOException while attempting to list files.", e);
    }
  }

  private Stream<GeoParquetGroup> streamGeoParquetGroups(boolean inParallel) {
    Spliterator<GeoParquetGroup> spliterator =
        new GeoParquetSpliterator(files, envelope, configuration, 0, files.size());
    return StreamSupport.stream(spliterator, inParallel);
  }

  public Stream<GeoParquetGroup> read() {
    return streamGeoParquetGroups(false);
  }

  public Stream<GeoParquetGroup> readParallel() {
    return streamGeoParquetGroups(true);
  }

  @Override
  public void close() throws IOException {
    // TODO: Implement close
  }

  private record FileInfo(
      FileStatus file,
      long recordCount,
      Map<String, String> keyValueMetadata,
      MessageType messageType,
      GeoParquetMetadata metadata,
      GeoParquetSchema geoParquetSchema) {

  }

}
