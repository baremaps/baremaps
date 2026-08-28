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



import com.baremaps.geocoder.GeocoderConstants;
import com.google.common.base.Strings;
import java.text.ParseException;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.expressions.Expression;
import org.apache.lucene.expressions.SimpleBindings;
import org.apache.lucene.expressions.js.JavascriptCompiler;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Utility class to build a Lucene {@link Query}.
 */
public class GeonamesQueryBuilder {

  private final Analyzer analyzer;

  private String queryText;

  private String countryCode = "";

  private boolean scoringByPopulation;

  private boolean andOperator;

  private String featureCode;

  public GeonamesQueryBuilder() {
    this(GeocoderConstants.ANALYZER);
  }

  public GeonamesQueryBuilder(Analyzer analyzer) {
    this.analyzer = analyzer;
  }

  public GeonamesQueryBuilder queryText(String queryText) {
    this.queryText = queryText;
    return this;
  }

  public GeonamesQueryBuilder countryCode(String countryCode) {
    this.countryCode = countryCode;
    return this;
  }

  /**
   * The scoring will take into account the population
   */
  public GeonamesQueryBuilder scoringByPopulation() {
    this.scoringByPopulation = true;
    return this;
  }

  public GeonamesQueryBuilder featureCode(String featureCode) {
    this.featureCode = featureCode;
    return this;
  }

  /**
   * The queryText will be parsed with AND operator between terms instead of OR.
   */
  public GeonamesQueryBuilder andOperator() {
    this.andOperator = true;
    return this;
  }

  public Query build() throws ParseException {
    var builder = new BooleanQuery.Builder();

    if (queryText != null) {
      var queryTextEsc = QueryParserBase.escape(queryText);
      if (!queryTextEsc.isBlank()) {
        // Changing the fields here might affect queries using queryText.
        var fieldWeights = Map.of("name", 1f, "asciiname", 1f, "country", 1f, "countryCode", 1f);
        var parser = new SimpleQueryParser(analyzer, fieldWeights);
        if (andOperator) {
          // AND operator between query terms parsed instead of default OR
          parser.setDefaultOperator(BooleanClause.Occur.MUST);
        }
        var termsQuery = parser.parse(queryTextEsc);
        // at least one terms of the queryText must be present
        builder.add(termsQuery, BooleanClause.Occur.MUST);
      }
    }

    if (countryCode != null) {
      var countryCodeEsc = QueryParserBase.escape(countryCode);
      if (!countryCodeEsc.isBlank()) {
        var countryCodeQuery = new TermQuery(new Term("countryCode", countryCodeEsc));
        builder.add(countryCodeQuery, BooleanClause.Occur.MUST);
      }
    }


    if (!Strings.isNullOrEmpty(featureCode)) {
      var featureCodeQuery = new TermQuery(new Term("featureCode", featureCode));
      builder.add(featureCodeQuery, BooleanClause.Occur.MUST);
    }

    if (scoringByPopulation) {
      var query = builder.build();
      // ln(1+population) to tolerate entries with population=0
      Expression expr = JavascriptCompiler.compile("_score + ln(1+population)");

      var bindings = new SimpleBindings();
      bindings.add("_score", DoubleValuesSource.SCORES);
      bindings.add("population", DoubleValuesSource.fromIntField("population"));

      return new FunctionScoreQuery(
          query,
          expr.getDoubleValuesSource(bindings));
    }
    return builder.build();
  }
}
