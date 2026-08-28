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



import com.baremaps.utils.IsoCountriesUtils;
import java.util.function.Function;
import org.apache.lucene.document.*;

/**
 * Maps a {@link GeonamesEntry} to a Lucene {@link Document}.
 */
public class GeonamesDocumentMapper implements Function<GeonamesEntry, Document> {

  @Override
  public Document apply(GeonamesEntry entry) {
    Document document = new Document();
    document.add(new TextField("name", entry.getName(), Field.Store.YES));
    document.add(new TextField("country", IsoCountriesUtils.getCountry(entry.getCountryCode()),
        Field.Store.YES));
    // countryCode is not analyzed and thus must be queried using uppercase
    document.add(new StringField("countryCode", entry.getCountryCode(), Field.Store.YES));
    document.add(new LatLonPoint("point", entry.getLatitude(), entry.getLongitude()));
    document.add(new StoredField("longitude", entry.getLongitude()));
    document.add(new StoredField("latitude", entry.getLatitude()));
    document.add(new TextField("asciiname", entry.getAsciiname(), Field.Store.YES));
    document.add(new StoredField("alternatenames", entry.getAlternatenames()));
    document.add(new StringField("featureClass", entry.getFeatureClass(), Field.Store.YES));
    document.add(new StringField("featureCode", entry.getFeatureCode(), Field.Store.YES));
    document.add(new StoredField("cc2", entry.getCc2()));
    document.add(new StoredField("admin1Code", entry.getAdmin1Code()));
    document.add(new StoredField("admin2Code", entry.getAdmin2Code()));
    document.add(new StoredField("admin3Code", entry.getAdmin3Code()));
    document.add(new StoredField("admin4Code", entry.getAdmin4Code()));
    document.add(new NumericDocValuesField("population", entry.getPopulation()));
    document.add(new StoredField("population", entry.getPopulation()));
    if (entry.getElevation() != null) {
      document.add(new StoredField("elevation", entry.getElevation()));
    }
    document.add(new StoredField("dem", entry.getDem()));
    document.add(new StoredField("timezone", entry.getTimezone()));
    document.add(new StoredField("modificationDate", entry.getModificationDate()));
    return document;
  }
}
