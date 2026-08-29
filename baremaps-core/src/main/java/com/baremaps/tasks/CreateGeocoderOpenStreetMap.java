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

package com.baremaps.tasks;

import com.baremaps.data.stream.StreamUtils;
import com.baremaps.geocoder.GeocoderConstants;
import com.baremaps.geocoder.openstreetmap.OpenStreetMapEntityConsumer;
import com.baremaps.openstreetmap.GeometryOptions;
import com.baremaps.openstreetmap.pbf.PbfEntityReader;
import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Experimental feature.
 */
public class CreateGeocoderOpenStreetMap implements Task {

  private static final Logger logger = LoggerFactory.getLogger(CreateGeocoderOpenStreetMap.class);

  private Path file;
  private Path indexDirectory;

  /**
   * Constructs an {@code CreateGeocoderOpenStreetMap}.
   */
  public CreateGeocoderOpenStreetMap() {}

  /**
   * Constructs an {@code CreateGeocoderOpenStreetMap}.
   * 
   * @param file the OSM PBF file
   * @param indexDirectory the index directory
   */
  public CreateGeocoderOpenStreetMap(Path file, Path indexDirectory) {
    this.file = file;
    this.indexDirectory = indexDirectory;
  }

  @Override
  public void execute(WorkflowContext context) throws Exception {
    var path = file.toAbsolutePath();

    var directory = FSDirectory.open(indexDirectory);
    var config = new IndexWriterConfig(GeocoderConstants.ANALYZER);

    try (var coordinateMap = context.getCoordinateMap();
        var referenceMap = context.getReferenceMap();
        var indexWriter = new IndexWriter(directory, config)) {
      var importer = new OpenStreetMapEntityConsumer(indexWriter);
      execute(
          path,
          coordinateMap,
          referenceMap,
          importer);
    }
  }

  public static void execute(
      Path path,
      Map<Long, Coordinate> coordinateMap,
      Map<Long, List<Long>> referenceMap,
      OpenStreetMapEntityConsumer importer) throws IOException {

    // configure the block reader
    // Geometries stay in WGS 84, which is what the Lucene API expects
    var reader = new PbfEntityReader(new GeometryOptions(coordinateMap, referenceMap));

    try (var input = Files.newInputStream(path)) {
      StreamUtils.parallel(reader.read(input)).forEach(importer);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new StringJoiner(", ", CreateGeocoderOpenStreetMap.class.getSimpleName() + "[", "]")
        .add("file=" + file)
        .add("indexDirectory=" + indexDirectory)
        .toString();
  }
}
