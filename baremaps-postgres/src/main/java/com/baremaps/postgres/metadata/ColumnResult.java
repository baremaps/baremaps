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

package com.baremaps.postgres.metadata;

public record ColumnResult(
    String tableCat,
    String tableSchem,
    String tableName,
    String columnName,
    int dataType,
    String typeName,
    int columnSize,
    int decimalDigits,
    int numPrecRadix,
    int nullable,
    String remarks,
    String columnDef,
    int sqlDataType,
    int sqlDateTimeSub,
    int charOctetLenght,
    int ordinalPosition,
    String isNullable,
    String scopeCatalog,
    String scopeSchema,
    String scopeTable,
    short sourceDataType,
    String isAutoIncrement,
    String isGeneratedColumn) {
}
