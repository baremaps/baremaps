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

package com.baremaps.calcite.data;

import java.io.Serializable;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.type.SqlTypeName;

/**
 * A column in a table.
 */
public interface DataColumn extends Serializable {

  /**
   * Returns the name of the column.
   *
   * @return the name of the column
   */
  String name();

  /**
   * Returns the cardinality of the column. The cardinality defines if the column is required,
   * optional, or repeated.
   *
   * @return the cardinality of the column
   */
  Cardinality cardinality();

  /**
   * Returns true if the column is required.
   *
   * @return true if the column is required
   */
  default boolean isRequired() {
    return cardinality() == Cardinality.REQUIRED;
  }

  /**
   * Returns true if the column is optional.
   *
   * @return true if the column is optional
   */
  default boolean isOptional() {
    return cardinality() == Cardinality.OPTIONAL;
  }

  /**
   * Returns true if the column is repeated.
   *
   * @return true if the column is repeated
   */
  default boolean isRepeated() {
    return cardinality() == Cardinality.REPEATED;
  }

  enum Cardinality {
    REQUIRED,
    OPTIONAL,
    REPEATED
  }

  /**
   * Returns the SQL type name of the column.
   *
   * @return the SQL type name of the column
   */
  SqlTypeName sqlTypeName();

  /**
   * Returns the RelDataType of the column.
   *
   * @return the RelDataType of the column
   */
  RelDataType relDataType();

  /**
   * Returns the Class that represents the Java type of this column.
   *
   * @return the Class that represents the Java type of this column
   */
  Class<?> javaType();
}
