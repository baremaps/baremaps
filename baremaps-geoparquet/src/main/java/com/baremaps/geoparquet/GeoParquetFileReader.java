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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
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
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.locationtech.jts.geom.Envelope;

/**
 * Reads the {@link GeoParquetGroup}s of a single Parquet file, one row group at a time.
 * <p>
 * The envelope, when there is one, is applied twice over: once against the bounding box the file
 * declares in its metadata, which lets whole files be skipped without reading any data, and once
 * against the bounding box of each record. Both derive from the same envelope, so that the two
 * cannot drift apart.
 */
class GeoParquetFileReader implements Closeable {

  private final ParquetFileReader fileReader;
  private final MessageColumnIO columnIO;
  private final GeoParquetGroupRecordMaterializer materializer;
  private final Filter filter;

  private RecordReader<GeoParquetGroup> recordReader;
  private long rowsRemainingInGroup;

  /**
   * Opens a file for reading.
   *
   * @param file the file to read
   * @param configuration the configuration
   * @param envelope the envelope to filter the records, which may be null
   * @return the reader, or null when the bounding box of the file cannot intersect the envelope
   * @throws IOException if the file cannot be opened
   */
  static GeoParquetFileReader open(FileStatus file, Configuration configuration, Envelope envelope)
      throws IOException {
    ParquetFileReader fileReader =
        ParquetFileReader.open(HadoopInputFile.fromPath(file.getPath(), configuration));
    try {
      FileMetaData fileMetaData = fileReader.getFooter().getFileMetaData();
      GeoParquetMetadata metadata = GeoParquetMetadata.read(fileMetaData.getKeyValueMetaData());
      if (!intersects(metadata, envelope)) {
        fileReader.close();
        return null;
      }
      return new GeoParquetFileReader(fileReader, fileMetaData.getSchema(), metadata, envelope);
    } catch (IOException | RuntimeException e) {
      fileReader.close();
      throw e;
    }
  }

  private GeoParquetFileReader(
      ParquetFileReader fileReader,
      MessageType schema,
      GeoParquetMetadata metadata,
      Envelope envelope) {
    this.fileReader = fileReader;
    this.columnIO = new ColumnIOFactory().getColumnIO(schema);
    this.materializer = new GeoParquetGroupRecordMaterializer(schema, metadata);
    FilterPredicate predicate = predicate(schema, envelope);
    this.filter = predicate == null ? FilterCompat.NOOP : FilterCompat.get(predicate);
  }

  /**
   * Returns the next group of the file, or null once the file is exhausted. Records rejected by the
   * envelope are skipped here, so that callers only ever see matching groups.
   *
   * @return the next group
   * @throws IOException if the file cannot be read
   */
  GeoParquetGroup read() throws IOException {
    while (true) {
      while (rowsRemainingInGroup == 0) {
        PageReadStore pages = fileReader.readNextRowGroup();
        if (pages == null) {
          return null;
        }
        rowsRemainingInGroup = pages.getRowCount();
        recordReader = columnIO.getRecordReader(pages, materializer, filter);
      }
      rowsRemainingInGroup--;
      GeoParquetGroup group = recordReader.read();
      if (group != null) {
        return group;
      }
    }
  }

  @Override
  public void close() throws IOException {
    fileReader.close();
  }

  private static boolean intersects(GeoParquetMetadata metadata, Envelope envelope) {
    if (envelope == null || metadata == null) {
      return true;
    }
    Envelope fileEnvelope = metadata.envelope();
    return fileEnvelope == null || fileEnvelope.intersects(envelope);
  }

  /**
   * Builds the predicate that keeps the records whose bounding box intersects the envelope, or null
   * when the file does not carry a bounding box the predicate could be expressed against.
   */
  private static FilterPredicate predicate(MessageType schema, Envelope envelope) {
    if (envelope == null || envelope.isNull()
        || envelope.equals(new Envelope(-180, 180, -90, 90))) {
      return null;
    }
    PrimitiveTypeName bound = boundType(schema);
    if (bound == null) {
      return null;
    }
    // Two boxes intersect unless one lies entirely on one side of the other.
    return FilterApi.and(
        FilterApi.and(
            atMost(bound, "bbox.xmin", envelope.getMaxX()),
            atLeast(bound, "bbox.xmax", envelope.getMinX())),
        FilterApi.and(
            atMost(bound, "bbox.ymin", envelope.getMaxY()),
            atLeast(bound, "bbox.ymax", envelope.getMinY())));
  }

  /**
   * Returns the type of the four bounds of the bbox column, or null when the schema has no bbox
   * column made of four bounds of one and the same floating point type.
   */
  private static PrimitiveTypeName boundType(MessageType schema) {
    if (!schema.containsField("bbox") || schema.getType("bbox").isPrimitive()) {
      return null;
    }
    GroupType bbox = schema.getType("bbox").asGroupType();
    if (bbox.getFieldCount() != 4
        || !bbox.containsField("xmin")
        || !bbox.containsField("ymin")
        || !bbox.containsField("xmax")
        || !bbox.containsField("ymax")) {
      return null;
    }
    List<Type> bounds = bbox.getFields();
    if (bounds.stream().anyMatch(bound -> !bound.isPrimitive())) {
      return null;
    }
    PrimitiveTypeName type = bounds.get(0).asPrimitiveType().getPrimitiveTypeName();
    if (type != PrimitiveTypeName.DOUBLE && type != PrimitiveTypeName.FLOAT) {
      return null;
    }
    return bounds.stream()
        .allMatch(bound -> bound.asPrimitiveType().getPrimitiveTypeName() == type) ? type : null;
  }

  // Narrowing a bound to a float rounds to the nearest value, which can round past the bound and
  // drop records that do match. Nudging it one ulp outwards keeps the comparison conservative.

  private static FilterPredicate atMost(PrimitiveTypeName type, String column, double bound) {
    return switch (type) {
      case DOUBLE -> FilterApi.ltEq(FilterApi.doubleColumn(column), bound);
      case FLOAT -> FilterApi.ltEq(FilterApi.floatColumn(column), Math.nextUp((float) bound));
      default -> throw new GeoParquetException("Unexpected type of bound: " + type);
    };
  }

  private static FilterPredicate atLeast(PrimitiveTypeName type, String column, double bound) {
    return switch (type) {
      case DOUBLE -> FilterApi.gtEq(FilterApi.doubleColumn(column), bound);
      case FLOAT -> FilterApi.gtEq(FilterApi.floatColumn(column), Math.nextDown((float) bound));
      default -> throw new GeoParquetException("Unexpected type of bound: " + type);
    };
  }
}
