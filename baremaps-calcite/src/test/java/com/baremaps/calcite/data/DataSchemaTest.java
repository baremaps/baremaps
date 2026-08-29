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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Persists two tables in a directory and queries them back through a {@link DataSchema}. */
class DataSchemaTest {

  @TempDir
  Path directory;

  private final RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();

  @BeforeEach
  void setUp() throws Exception {
    try (DataModifiableTable cities = DataModifiableTable.create(directory.resolve("cities"),
        "cities", typeFactory.builder()
            .add("city", SqlTypeName.VARCHAR)
            .add("country", SqlTypeName.VARCHAR)
            .add("population", SqlTypeName.INTEGER)
            .build())) {
      cities.rows().add(new Object[] {"Paris", "France", 2_161_000});
      cities.rows().add(new Object[] {"Lyon", "France", 513_000});
      cities.rows().add(new Object[] {"Berlin", "Germany", 3_645_000});
    }
    try (DataModifiableTable countries = DataModifiableTable.create(
        directory.resolve("countries"), "countries", typeFactory.builder()
            .add("country", SqlTypeName.VARCHAR)
            .add("continent", SqlTypeName.VARCHAR)
            .add("population", SqlTypeName.INTEGER)
            .build())) {
      countries.rows().add(new Object[] {"France", "Europe", 67_000_000});
      countries.rows().add(new Object[] {"Germany", "Europe", 83_000_000});
    }
  }

  private Connection connect() throws Exception {
    Properties info = new Properties();
    info.setProperty("lex", "MYSQL");
    Connection connection = DriverManager.getConnection("jdbc:calcite:", info);
    CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
    calciteConnection.getRootSchema().add("data",
        new DataSchema(directory.toFile(), calciteConnection.getTypeFactory()));
    return connection;
  }

  @Test
  @Tag("integration")
  void tablesAreDiscovered() {
    DataSchema schema = new DataSchema(directory.toFile(), typeFactory);
    assertEquals(2, schema.getTableMap().size());
    assertTrue(schema.getTableMap().containsKey("cities"));
    assertTrue(schema.getTableMap().containsKey("countries"));
  }

  @Test
  @Tag("integration")
  void filter() throws Exception {
    try (Connection connection = connect();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(
            "SELECT city FROM data.cities WHERE country = 'France' ORDER BY city")) {
      assertTrue(resultSet.next());
      assertEquals("Lyon", resultSet.getString(1));
      assertTrue(resultSet.next());
      assertEquals("Paris", resultSet.getString(1));
      assertFalse(resultSet.next());
    }
  }

  @Test
  @Tag("integration")
  void join() throws Exception {
    try (Connection connection = connect();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(
            "SELECT c.city, co.continent FROM data.cities c "
                + "JOIN data.countries co ON c.country = co.country "
                + "WHERE co.continent = 'Europe'")) {
      int rows = 0;
      while (resultSet.next()) {
        assertEquals("Europe", resultSet.getString("continent"));
        rows++;
      }
      assertEquals(3, rows);
    }
  }
}
