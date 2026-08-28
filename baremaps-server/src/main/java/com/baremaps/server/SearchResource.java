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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.annotation.Default;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.Param;
import com.linecorp.armeria.server.annotation.ProducesJson;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resource that searches for entities in the database.
 */
public class SearchResource {

  private static final Logger logger = LoggerFactory.getLogger(SearchResource.class);

  private static final String SEARCH_QUERY =
      """
          SELECT id, tags, st_asewkt(st_transform(geom, 'EPSG:4326')), ts_rank_cd(to_tsvector('english', tags), query) as rank
          FROM osm_entities, phraseto_tsquery('english', ?) as query
          WHERE to_tsvector('english', tags) @@ query
          ORDER BY rank DESC
          LIMIT ?;
          """;

  private final DataSource dataSource;

  public SearchResource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  record SearchResponse(List<SearchResult> results) {
  }

  record SearchResult(long id, JsonNode tags, String wkt, double score) {
  }

  @Get("/api/search")
  @ProducesJson
  public HttpResponse search(
      @Param("query") String queryText,
      @Param("limit") @Default("10") int limit) {
    try (var connection = dataSource.getConnection();
        var statement = connection.prepareStatement(SEARCH_QUERY)) {
      statement.setString(1, queryText);
      statement.setInt(2, limit);
      var list = new ArrayList<SearchResult>();
      try (var result = statement.executeQuery()) {
        while (result.next()) {
          var id = result.getLong(1);
          var json = result.getString(2);
          var tags = new ObjectMapper().readTree(json);
          var wkt = result.getString(3);
          var rank = result.getDouble(4);
          list.add(new SearchResult(id, tags, wkt, rank));
        }
      }
      var response = new SearchResponse(list);
      return HttpResponse.ofJson(response);
    } catch (SQLException | JsonProcessingException e) {
      logger.error("Error while searching", e);
      return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
