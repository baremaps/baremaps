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

package com.baremaps.geocoder.openstreetmap;

import com.baremaps.geocoder.GeocoderConstants;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

public class OpenStreetMapQuery {

  private final String query;

  public OpenStreetMapQuery(String query) {
    this.query = query;
  }

  public Query build() {
    var builder = new BooleanQuery.Builder();
    var queryTextEsc = QueryParserBase.escape(query);

    var parser = new SimpleQueryParser(GeocoderConstants.ANALYZER, OsmTags.NAME.key());
    var termsQuery = parser.parse(queryTextEsc);
    // at least one terms of the queryText must be present
    builder.add(termsQuery, BooleanClause.Occur.MUST);
    return builder.build();
  }
}
