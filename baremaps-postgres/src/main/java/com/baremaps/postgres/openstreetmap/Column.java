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

package com.baremaps.postgres.openstreetmap;

/**
 * A column of a repository table, and the three forms its name takes in the statements
 * {@link AbstractRepository} generates.
 *
 * <p>
 * Two Postgres types cannot be read or written as themselves: a geometry has to be asked for as
 * EWKB, and a jsonb parameter has to be cast from the text a driver binds. Carrying those forms on
 * the column removes the only place where the statement builder would otherwise have to ask what
 * type it is looking at.
 *
 * @param name the column name
 * @param sqlType the type it is declared with
 * @param selection how it is named in a select list
 * @param parameter how a value is bound to it in an insert
 */
record Column(String name, String sqlType, String selection, String parameter) {

  /** A column read and written as itself. */
  static Column of(String name, String sqlType) {
    return new Column(name, sqlType, name, "?");
  }

  /** A jsonb column, whose parameter is bound as text and cast by the server. */
  static Column jsonb(String name) {
    return new Column(name, "jsonb", name, "cast(? AS jsonb)");
  }

  /** A geometry column, selected as EWKB so that it round-trips through the JTS serializer. */
  static Column geometry(String name, String sqlType) {
    return new Column(name, sqlType, "st_asewkb(%s)".formatted(name), "?");
  }
}
