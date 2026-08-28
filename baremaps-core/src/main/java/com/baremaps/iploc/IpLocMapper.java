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
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
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
  @SuppressWarnings({"squid:S3776", "squid:S1192"})
  public Optional<IpLocObject> apply(RpslObject rpslObject) {
    try {
      if (rpslObject.attributes().isEmpty()) {
        return Optional.empty();
      }

      if (!RpslUtils.isInetnum(rpslObject)) {
        return Optional.empty();
      }

      var inetnum = rpslObject.attributes().get(0);
      var ipRange = IpResourceRange.parse(inetnum.value());
      var start = InetAddresses.forString(ipRange.getStart().toString());
      var end = InetAddresses.forString(ipRange.getEnd().toString());
      var inetRange = new InetRange(start, end);

      var attributes = rpslObject.asMap();

      // Use a default name if there is no netname
      var network = String.join(", ", attributes.getOrDefault("netname", List.of("unknown")));
      var geoloc = attributes.containsKey("geoloc")
          ? String.join(", ", attributes.getOrDefault("geoloc", List.of()))
          : null;
      var country = attributes.containsKey("country")
          ? String.join(", ", attributes.getOrDefault("country", List.of()))
          : null;
      var source = attributes.containsKey("source")
          ? String.join(", ", attributes.getOrDefault("source", List.of()))
          : null;

      // If there is a geoloc field, we use the latitude and longitude provided
      if (attributes.containsKey("geoloc")) {
        var location = stringToCoordinate(geoloc);
        if (location.isPresent()) {
          return Optional.of(new IpLocObject(
              geoloc,
              inetRange,
              location.get(),
              network,
              country,
              source,
              IpLocPrecision.GEOLOC));
        }
      }

      // If there is a country, we use that with a cherry-picked list of fields to query the
      // geocoder with confidence to find a relevant precise location,
      // in the worst case the error is within a country
      List<String> searchedFields = List.of("descr", "netname");
      // at least one of a searchedField is present and the country is present.
      if (attributes.keySet().stream().anyMatch(searchedFields::contains)
          && attributes.containsKey("country")) {
        // build a query text string out of the cherry-picked fields
        var queryTextBuilder = new StringBuilder();
        for (String field : searchedFields) {
          var value = attributes.containsKey(field)
              ? String.join(", ", attributes.getOrDefault(field, List.of()))
              : null;
          if (!Strings.isNullOrEmpty(value)) {
            queryTextBuilder.append(attributes.get(field)).append(" ");
          }
        }

        String queryText = queryTextBuilder.toString();
        var location = findLocationInCountry(queryText, country);
        if (location.isPresent()) {
          return Optional.of(new IpLocObject(
              queryText,
              inetRange,
              location.get(),
              network,
              country,
              source,
              IpLocPrecision.GEOCODER));
        }
      }

      // If there is a country get the location of country
      if (attributes.containsKey("country")) {
        var location = findCountryLocation(country);
        if (location.isPresent()) {
          return Optional.of(new IpLocObject(
              country,
              inetRange,
              location.get(),
              network,
              country,
              source,
              IpLocPrecision.COUNTRY));
        }
      }

      return Optional.of(new IpLocObject(
          null,
          inetRange,
          new Coordinate(),
          network,
          null,
          source,
          IpLocPrecision.WORLD));
    } catch (Exception e) {
      logger.warn("Error while mapping RPSL object to IP loc object", e);
      logger.warn("RPSL object attributes:");
      rpslObject.attributes().forEach(attribute -> {
        var name = attribute.name();
        var value = attribute.value();
        if (value.length() > 100) {
          value = value.substring(0, 100).concat("...");
        }
        logger.warn("  {} = {}", name, value);
      });
      return Optional.empty();
    }
  }

  private Optional<Coordinate> findCountryLocation(String country)
      throws IOException, ParseException {
    GeonamesQueryBuilder geonamesQuery = new GeonamesQueryBuilder().featureCode("PCLI");
    if (IsoCountriesUtils.containsCountry(country.toUpperCase())) {
      geonamesQuery.countryCode(country.toUpperCase());
    } else {
      geonamesQuery.queryText(country).build();
    }
    return findLocation(geonamesQuery.build());
  }

  private Optional<Coordinate> findLocationInCountry(String terms, String countryCode)
      throws IOException, ParseException {
    var geonamesQuery =
        new GeonamesQueryBuilder().queryText(terms).countryCode(countryCode).build();
    return findLocation(geonamesQuery);
  }

  /**
   * Uses the geocoder to find the location of the specified query
   *
   * @return an {@code Optional} containing the location of the search terms
   * @throws IOException if an I/O error occurs
   */
  private Optional<Coordinate> findLocation(Query query)
      throws IOException {
    var indexSearcher = searcherManager.acquire();

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
