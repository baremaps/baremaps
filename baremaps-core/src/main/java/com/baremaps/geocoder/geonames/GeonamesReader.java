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

package com.baremaps.geocoder.geonames;



import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A reader for the Geonames database.
 */
public class GeonamesReader {

  public Stream<GeonamesEntry> stream(InputStream inputStream) throws IOException {
    CsvMapper mapper = new CsvMapper();

    CsvSchema schema = CsvSchema.builder()
        .addColumn("geonameid")
        .addColumn("name")
        .addColumn("asciiname")
        .addColumn("alternatenames")
        .addColumn("latitude")
        .addColumn("longitude")
        .addColumn("featureClass")
        .addColumn("featureCode")
        .addColumn("countryCode")
        .addColumn("cc2")
        .addColumn("admin1Code")
        .addColumn("admin2Code")
        .addColumn("admin3Code")
        .addColumn("admin4Code")
        .addColumn("population")
        .addColumn("elevation")
        .addColumn("dem")
        .addColumn("timezone")
        .addColumn("modificationDate")
        .build()
        .withColumnSeparator('\t')
        .withoutQuoteChar();

    var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    MappingIterator<GeonamesEntry> recordIterator = mapper
        .readerFor(GeonamesEntry.class)
        .with(schema)
        .readValues(reader);
    Spliterator<GeonamesEntry> recordSpliterator =
        Spliterators.spliteratorUnknownSize(recordIterator, 0);

    return StreamSupport.stream(recordSpliterator, false);
  }
}
