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

package com.baremaps.server;

import static com.google.common.net.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;

import com.baremaps.geocoder.geonames.GeonamesQueryBuilder;
import com.baremaps.openstreetmap.stream.StreamException;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.server.annotation.Default;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.Param;
import com.linecorp.armeria.server.annotation.ProducesJson;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resource that provides access to the geocoder.
 */
public class GeocoderResource {

  private static final Logger logger = LoggerFactory.getLogger(GeocoderResource.class);

  record GeocoderResponse(List<GeocoderResult> results) {
  }

  record GeocoderResult(float score, Map<String, Object> data) {
  }

  private final SearcherManager searcherManager;

  public GeocoderResource(SearcherManager searcherManager) {
    this.searcherManager = searcherManager;
  }

  @Get("/api/geocoder")
  @ProducesJson
  public HttpResponse searchLocations(
      @Param("queryText") String queryText,
      @Param("countryCode") @Default("") String countryCode,
      @Param("limit") @Default("10") int limit) {
    if (queryText == null) {
      return HttpResponse.of(HttpStatus.BAD_REQUEST);
    }
    try {
      IndexSearcher searcher = searcherManager.acquire();
      try {
        // Querying to search location uses AND operator between terms such as every term "adds up"
        // Examples of queryText:
        // - "paris", returns paris in france in first results (i.e because of scoring with
        // population)
        // - "paris brazil", returns paris in brazil and not paris in france.
        var query = new GeonamesQueryBuilder()
            .queryText(queryText).countryCode(countryCode).scoringByPopulation()
            .andOperator()
            .build();

        var result = searcher.search(query, limit);
        var results =
            Arrays.stream(result.scoreDocs).map(scoreDoc -> asResult(searcher, scoreDoc)).toList();

        var headers = ResponseHeaders.builder(200)
            .add(CONTENT_TYPE, APPLICATION_JSON)
            .add(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
            .build();

        return HttpResponse.ofJson(headers, new GeocoderResponse(results));
      } catch (IllegalArgumentException e) {
        logger.warn("Illegal input while processing request", e);
        return HttpResponse.of(HttpStatus.BAD_REQUEST);
      } catch (IOException | ParseException e) {
        logger.error("Error while processing request", e);
        return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR);
      } finally {
        searcherManager.release(searcher);
      }
    } catch (Exception e) {
      logger.error("Error while processing request", e);
      return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private GeocoderResult asResult(IndexSearcher indexSearcher, ScoreDoc scoreDoc) {
    try {
      var document = indexSearcher.doc(scoreDoc.doc);
      var data = new HashMap<String, Object>();
      for (var field : document.getFields()) {
        if (field.numericValue() != null) {
          data.put(field.name(), field.numericValue());
        } else if (field.stringValue() != null) {
          data.put(field.name(), field.stringValue());
        }
      }
      return new GeocoderResult(scoreDoc.score, data);
    } catch (IOException e) {
      throw new StreamException(e);
    }
  }
}
