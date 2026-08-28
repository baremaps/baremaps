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
import java.io.IOException;
import java.util.List;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.filter2.compat.FilterCompat;
import org.apache.parquet.filter2.compat.FilterCompat.Filter;
import org.apache.parquet.filter2.predicate.FilterApi;
import org.apache.parquet.filter2.predicate.FilterPredicate;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.FileMetaData;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.locationtech.jts.geom.Envelope;

/**
 * A {@link Spliterator} for {@link GeoParquetGroup}s stored in Parquet files. The envelope is used
 * to filter the records based on their bounding box.
 */
class GeoParquetSpliterator implements Spliterator<GeoParquetGroup> {

  private final List<FileStatus> files;
  private final Configuration configuration;
  private final Envelope envelope;

  private ParquetFileReader fileReader;
  private int fileStartIndex;
  private int fileEndIndex;
  private MessageType schema;
  private GeoParquetMetadata metadata;
  private MessageColumnIO columnIO;
  private RecordReader<GeoParquetGroup> recordReader;
  private int currentRowGroup;
  private long rowsReadInGroup;
  private long rowsInCurrentGroup;

  /**
   * Constructs a new {@code GeoParquetSpliterator} with the specified files, envelope,
   * configuration, file start index and file end index.
   * 
   * @param files the files
   * @param envelope the envelope
   * @param configuration the configuration
   * @param fileStartIndex the file start index
   * @param fileEndIndex the file end index
   */
  GeoParquetSpliterator(
      List<FileStatus> files,
      Envelope envelope,
      Configuration configuration,
      int fileStartIndex,
      int fileEndIndex) {
    this.files = files;
    this.configuration = configuration;
    this.envelope = envelope;
    this.fileStartIndex = fileStartIndex;
    this.fileEndIndex = fileEndIndex;
    setupReaderForNextFile();
  }

  private void setupReaderForNextFile() {
    closeCurrentReader();

    while (fileStartIndex < fileEndIndex) {
      FileStatus fileStatus = files.get(fileStartIndex++);
      try {
        InputFile inputFile = HadoopInputFile.fromPath(fileStatus.getPath(), configuration);
        fileReader = ParquetFileReader.open(inputFile);

        FileMetaData fileMetaData = fileReader.getFooter().getFileMetaData();

        schema = fileMetaData.getSchema();
        metadata = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(fileMetaData.getKeyValueMetaData().get("geo"), GeoParquetMetadata.class);

        // Check if file's bbox overlaps with the envelope
        if (envelope != null && metadata != null && metadata.bbox() != null) {
          List<Double> fileBBox = metadata.bbox();
          if (fileBBox.size() == 4) {
            Envelope fileEnvelope = new Envelope(
                fileBBox.get(0), fileBBox.get(2), fileBBox.get(1), fileBBox.get(3));
            if (!fileEnvelope.intersects(envelope)) {
              // Skip this file and continue to the next one
              fileReader.close();
              fileReader = null;
              continue;
            }
          }
        }

        columnIO = new ColumnIOFactory().getColumnIO(schema);
        currentRowGroup = 0;
        rowsReadInGroup = 0;
        rowsInCurrentGroup = 0;
        advanceToNextRowGroup();
        return;
      } catch (IOException e) {
        throw new GeoParquetException("Failed to create reader for " + fileStatus, e);
      }
    }

    // No more files to process
    fileReader = null;
  }

  private void advanceToNextRowGroup() throws IOException {
    if (currentRowGroup >= fileReader.getRowGroups().size()) {
      setupReaderForNextFile();
      return;
    }

    PageReadStore pages = fileReader.readNextFilteredRowGroup();
    if (pages == null) {
      setupReaderForNextFile();
      return;
    }

    rowsInCurrentGroup = pages.getRowCount();
    rowsReadInGroup = 0;

    GeoParquetGroupRecordMaterializer materializer =
        new GeoParquetGroupRecordMaterializer(schema, metadata);

    FilterPredicate envelopeFilter = createEnvelopeFilter(schema, envelope);
    Filter filter = envelopeFilter == null ? FilterCompat.NOOP : FilterCompat.get(envelopeFilter);

    recordReader = columnIO.getRecordReader(pages, materializer, filter);
    currentRowGroup++;
  }

  private FilterPredicate createEnvelopeFilter(MessageType schema, Envelope envelope) {
    // Check whether the envelope is null or the world
    if (envelope == null
        || envelope.isNull()
        || envelope.equals(new Envelope(-180, 180, -90, 90))) {
      return null;
    }

    // Check whether the schema has a bbox field
    Type type = schema.getType("bbox");
    if (type == null) {
      return null;
    }

    // Check whether the bbox has the xmin, ymin, xmax, ymax fields
    GroupType bbox = type.asGroupType();
    if (bbox.getFieldCount() != 4
        || !bbox.containsField("xmin")
        || !bbox.containsField("ymin")
        || !bbox.containsField("xmax")
        || !bbox.containsField("ymax")) {
      return null;
    }

    // Check whether all fields are primitive types
    List<Type> types = bbox.getFields();
    if (types.stream().anyMatch(t -> !t.isPrimitive())) {
      return null;
    }

    // Check whether all fields are of the same type
    List<PrimitiveTypeName> typeNames = types.stream()
        .map(t -> t.asPrimitiveType().getPrimitiveTypeName())
        .toList();
    PrimitiveTypeName typeName = typeNames.get(0);
    if (!typeNames.stream().allMatch(typeName::equals)) {
      return null;
    }

    // Check whether the type is a float or a double
    if (typeName != PrimitiveTypeName.DOUBLE && typeName != PrimitiveTypeName.FLOAT) {
      return null;
    }

    // Initialize the filter predicate creator for the given type
    BiFunction<String, Number, FilterPredicate> filterPredicateCreator =
        (column, value) -> switch (typeName) {
        case DOUBLE -> FilterApi.gtEq(FilterApi.doubleColumn(column), value.doubleValue());
        case FLOAT -> FilterApi.gtEq(FilterApi.floatColumn(column), value.floatValue());
        default -> throw new IllegalStateException("Unexpected value: " + typeName);
        };

    // Create the filter predicate
    return FilterApi.and(
        FilterApi.and(
            filterPredicateCreator.apply("bbox.xmin", envelope.getMinX()),
            filterPredicateCreator.apply("bbox.xmax", envelope.getMaxX())),
        FilterApi.and(
            filterPredicateCreator.apply("bbox.ymin", envelope.getMinY()),
            filterPredicateCreator.apply("bbox.ymax", envelope.getMaxY())));
  }

  @Override
  public boolean tryAdvance(Consumer<? super GeoParquetGroup> action) {
    try {
      while (true) {
        if (fileReader == null) {
          return false;
        }

        if (rowsReadInGroup >= rowsInCurrentGroup) {
          advanceToNextRowGroup();
          continue;
        }

        GeoParquetGroup group = recordReader.read();
        rowsReadInGroup++;
        if (group != null) {
          action.accept(group);
        }

        return true;
      }
    } catch (IOException e) {
      closeCurrentReader();
      throw new GeoParquetException("IOException caught while trying to read the next record.", e);
    }
  }

  private void closeCurrentReader() {
    if (fileReader != null) {
      try {
        fileReader.close();
      } catch (IOException e) {
        throw new GeoParquetException("Failed to close ParquetFileReader.", e);
      } finally {
        fileReader = null;
      }
    }
  }

  @Override
  public Spliterator<GeoParquetGroup> trySplit() {
    int remainingFiles = fileEndIndex - fileStartIndex;
    if (remainingFiles <= 1) {
      return null;
    }
    int mid = fileStartIndex + remainingFiles / 2;
    GeoParquetSpliterator split =
        new GeoParquetSpliterator(files, envelope, configuration, mid, fileEndIndex);
    this.fileEndIndex = mid;
    return split;
  }

  @Override
  public long estimateSize() {
    // Return Long.MAX_VALUE as the actual number of elements is unknown
    return Long.MAX_VALUE;
  }

  @Override
  public int characteristics() {
    return NONNULL | IMMUTABLE;
  }

}
