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

package com.baremaps.calcite.postgres;

import com.baremaps.calcite.BaremapsDdlExecutor;
import org.apache.calcite.sql.parser.SqlParserImplFactory;

/**
 * Executes the Baremaps DDL against PostgreSQL.
 *
 * <p>
 * Use it with
 * {@code parserFactory=com.baremaps.calcite.postgres.PostgresDdlExecutor#PARSER_FACTORY} and
 * address objects inside a registered {@link PostgresSchema}, e.g.
 * {@code CREATE TABLE pg.roads AS SELECT ...}: the schema carries the {@code DataSource}.
 */
public final class PostgresDdlExecutor extends BaremapsDdlExecutor {

  @SuppressWarnings("unused") // used via reflection
  public static final SqlParserImplFactory PARSER_FACTORY =
      parserFactory(new PostgresDdlExecutor());

  private PostgresDdlExecutor() {
    super(new PostgresDdlBackend());
  }
}
