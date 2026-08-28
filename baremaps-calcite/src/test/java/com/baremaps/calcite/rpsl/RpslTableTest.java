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

package com.baremaps.calcite.rpsl;

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.testing.TestFiles;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.junit.jupiter.api.Test;

class RpslTableTest {

  private static final File SAMPLE_RPSL_FILE =
      TestFiles.RPSL_TXT.toFile();

  @Test
  void testSchemaVerification() throws IOException {
    // Create a RpslTable instance
    RpslTable rpslTable = new RpslTable(SAMPLE_RPSL_FILE);

    // Get the schema
    RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
    RelDataType rowType = rpslTable.getRowType(typeFactory);

    // Verify that the schema has the expected columns
    assertNotNull(rowType);
    assertTrue(rowType.getFieldCount() > 0);

    // Verify that specific columns exist
    boolean hasTypeColumn = false;
    boolean hasIdColumn = false;
    boolean hasInetnumColumn = false;

    for (int i = 0; i < rowType.getFieldCount(); i++) {
      String fieldName = rowType.getFieldList().get(i).getName();
      if (fieldName.equals("type")) {
        hasTypeColumn = true;
      } else if (fieldName.equals("id")) {
        hasIdColumn = true;
      } else if (fieldName.equals("inetnum")) {
        hasInetnumColumn = true;
      }
    }

    assertTrue(hasTypeColumn, "Schema should have a 'type' column");
    assertTrue(hasIdColumn, "Schema should have an 'id' column");
    assertTrue(hasInetnumColumn, "Schema should have an 'inetnum' column");
  }

  @Test
  void testSqlQueryWithRealRpslFile() throws Exception {
    // Create a RpslTable instance
    RpslTable rpslTable = new RpslTable(SAMPLE_RPSL_FILE);

    // Configure Calcite connection properties
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");

    // Set up a connection and register our table
    try (Connection connection = DriverManager.getConnection("jdbc:calcite:", info)) {
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      SchemaPlus rootSchema = calciteConnection.getRootSchema();

      // Register the table in the root schema
      rootSchema.add("rpsl", rpslTable);

      // Execute a simple query
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery(
              "SELECT type, id, inetnum FROM rpsl WHERE type = 'inetnum' LIMIT 5")) {

        // Verify that we get results
        assertTrue(resultSet.next(), "Should have at least one row");
      }
    }
  }
}
