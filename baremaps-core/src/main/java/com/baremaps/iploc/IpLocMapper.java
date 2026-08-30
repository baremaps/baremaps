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

package com.baremaps.iploc;


import com.baremaps.geocoder.geonames.GeonamesQueryBuilder;
import com.baremaps.rpsl.RpslObject;
import com.baremaps.rpsl.RpslUtils;
import com.baremaps.utils.IsoCountriesUtils;
import com.google.common.base.Strings;
import com.google.common.net.InetAddresses;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.ripe.ipresource.IpResourceRange;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherManager;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Generating pairs of IP address ranges and their locations into an SQLite database */
public class IpLocMapper implements Function<RpslObject, Optional<IpLocObject>> {

  private static final Logger logger = LoggerFactory.getLogger(IpLocMapper.class);

  private static final float SCORE_THRESHOLD = 0.1f;

  /** The attributes that describe a network well enough to hand to the geocoder. */
  private static final List<String> SEARCHED_FIELDS = List.of("descr", "netname");

  private final SearcherManager searcherManager;

  /**
   * Constructs an IpLocMapper with the specified geocoder used to find the locations of the
   * objects.
   *
   * @param searcherManager the geocoder that will be used to find the locations of the objects
   */
  public IpLocMapper(SearcherManager searcherManager) {
    this.searcherManager = searcherManager;
  }

  /**
   * Returns an {@code Optional} containing the {@code IpLocObject} associated with the specified
   * {@code NicObject} if it is an inetnum object, or an empty {@code Optional} otherwise.
   *
   * @param rpslObject the {@code NicObject}
   * @return an {@code Optional} containing the {@code IpLocObject} corresponding to the
   *         {@code NicObject}
   */
  @Override
  public Optional<IpLocObject> apply(RpslObject rpslObject) {
    if (rpslObject.attributes().isEmpty() || !RpslUtils.isInetnum(rpslObject)) {
      return Optional.empty();
    }
    try {
      var ipRange = IpResourceRange.parse(rpslObject.attributes().get(0).value());
      var inetRange = new InetRange(
          InetAddresses.forString(ipRange.getStart().toString()),
          InetAddresses.forString(ipRange.getEnd().toString()));
      return Optional.of(locate(rpslObject.asMap(), inetRange));
    } catch (Exception e) {
      logUnmappable(rpslObject, e);
      return Optional.empty();
    }
  }

  /**
   * Locates an inetnum, from the most precise source of location to the least: the coordinates the
   * object declares, then the geocoder narrowed down to the country it declares, then the country
   * itself. An object that declares none of them is placed at the origin.
   */
  private IpLocObject locate(Map<String, List<String>> attributes, InetRange inetRange)
      throws IOException, ParseException {
    // Use a default name if there is no netname
    var network = attribute(attributes, "netname", "unknown");
    var country = attribute(attributes, "country", null);
    var source = attribute(attributes, "source", null);

    var geoloc = attribute(attributes, "geoloc", null);
    if (geoloc != null) {
      var location = stringToCoordinate(geoloc);
      if (location.isPresent()) {
        return new IpLocObject(geoloc, inetRange, location.get(), network, country, source,
            IpLocPrecision.GEOLOC);
      }
    }

    if (country != null) {
      // Cherry-picked fields keep the geocoder query focused enough to trust its first hit; the
      // country restricts the error to that country in the worst case.
      var queryText = SEARCHED_FIELDS.stream()
          .map(field -> attribute(attributes, field, null))
          .filter(value -> !Strings.isNullOrEmpty(value))
          .collect(Collectors.joining(" "));
      if (!queryText.isBlank()) {
        var location = findLocation(
            new GeonamesQueryBuilder().queryText(queryText).countryCode(country).build());
        if (location.isPresent()) {
          return new IpLocObject(queryText, inetRange, location.get(), network, country, source,
              IpLocPrecision.GEOCODER);
        }
      }

      var location = findCountryLocation(country);
      if (location.isPresent()) {
        return new IpLocObject(country, inetRange, location.get(), network, country, source,
            IpLocPrecision.COUNTRY);
      }
    }

    return new IpLocObject(null, inetRange, new Coordinate(), network, null, source,
        IpLocPrecision.WORLD);
  }

  /** Returns the values of an attribute joined by a comma, or {@code fallback} if it has none. */
  private static String attribute(Map<String, List<String>> attributes, String name,
      String fallback) {
    var values = attributes.get(name);
    return values == null || values.isEmpty() ? fallback : String.join(", ", values);
  }

  private static void logUnmappable(RpslObject rpslObject, Exception e) {
    logger.warn("Error while mapping RPSL object to IP loc object", e);
    logger.warn("RPSL object attributes:");
    rpslObject.attributes().forEach(attribute -> {
      var value = attribute.value();
      if (value.length() > 100) {
        value = value.substring(0, 100).concat("...");
      }
      logger.warn("  {} = {}", attribute.name(), value);
    });
  }

  private Optional<Coordinate> findCountryLocation(String country)
      throws IOException, ParseException {
    var geonamesQuery = new GeonamesQueryBuilder().featureCode("PCLI");
    if (IsoCountriesUtils.containsCountry(country.toUpperCase())) {
      geonamesQuery.countryCode(country.toUpperCase());
    } else {
      geonamesQuery.queryText(country);
    }
    return findLocation(geonamesQuery.build());
  }

  /**
   * Uses the geocoder to find the location of the specified query
   *
   * @return an {@code Optional} containing the location of the search terms
   * @throws IOException if an I/O error occurs
   */
  private Optional<Coordinate> findLocation(Query query) throws IOException {
    var indexSearcher = searcherManager.acquire();
    try {
      var topDocs = indexSearcher.search(query, 1);
      if (topDocs.scoreDocs.length == 0) {
        return Optional.empty();
      }

      var scoreDoc = topDocs.scoreDocs[0];
      if (scoreDoc.score < SCORE_THRESHOLD) {
        return Optional.empty();
      }

      var document = indexSearcher.doc(scoreDoc.doc);
      var longitude = document.getField("longitude").numericValue().doubleValue();
      var latitude = document.getField("latitude").numericValue().doubleValue();

      return Optional.of(new Coordinate(longitude, latitude));
    } finally {
      // The manager keeps the underlying reader open until every searcher it handed out is back.
      searcherManager.release(indexSearcher);
    }
  }

  /**
   * Parse the geoloc in the given string and insert it in the database. The given geoloc is
   * represented by two doubles split by a space.
   *
   * @param geoloc the latitude/longitude coordinates in a string
   * @return an optional containing the location
   */
  private Optional<Coordinate> stringToCoordinate(String geoloc) {
    var doubleRegex = "(\\d+\\.\\d+)";
    var pattern = Pattern.compile("^" + doubleRegex + " " + doubleRegex + "$");
    var matcher = pattern.matcher(geoloc);
    if (matcher.find()) {
      double latitude = Double.parseDouble(matcher.group(1));
      double longitude = Double.parseDouble(matcher.group(2));
      return Optional.of(new Coordinate(longitude, latitude));
    }
    return Optional.empty();
  }
}
