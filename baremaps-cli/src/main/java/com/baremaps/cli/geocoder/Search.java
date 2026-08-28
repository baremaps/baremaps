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

package com.baremaps.cli.geocoder;



import com.baremaps.geocoder.geonames.GeonamesQueryBuilder;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "search",
    description = "Search geonames index.")
@SuppressWarnings("squid:S106")
public class Search implements Callable<Integer> {

  private static final Logger logger = LoggerFactory.getLogger(Search.class);

  @Option(
      names = {"--index"}, paramLabel = "INDEX", description = "The path to the lucene index.",
      required = true)
  private Path indexDirectory;

  @Option(
      names = {"--terms"}, paramLabel = "terms",
      description = "The terms to search in the index.", required = true)
  private String terms;

  @Option(
      names = {"--country"}, paramLabel = "COUNTRY", description = "The country code filter.",
      required = false)
  private String countryCode = "";

  @Option(
      names = {"--limit"}, paramLabel = "LIMIT",
      description = "The number of result to return.", required = false)
  private Integer limit = 10;

  @Override
  public Integer call() throws Exception {
    try (
        var directory = FSDirectory.open(indexDirectory);
        var searcherManager = new SearcherManager(directory, new SearcherFactory())) {
      var query = new GeonamesQueryBuilder().queryText(terms).countryCode(countryCode).build();
      var searcher = searcherManager.acquire();
      try {
        var result = searcher.search(query, limit);
        for (var hit : result.scoreDocs) {
          var document = searcher.doc(hit.doc);
          logger.info("{}", document);
        }
      } finally {
        searcherManager.release(searcher);
      }
    }

    return 0;
  }
}
