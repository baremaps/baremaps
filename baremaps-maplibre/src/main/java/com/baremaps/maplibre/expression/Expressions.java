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

package com.baremaps.maplibre.expression;



import com.baremaps.maplibre.vectortile.Feature;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.locationtech.jts.geom.*;

public interface Expressions {

  interface Expression<T> {

    String name();

    T evaluate(Feature feature);

  }

  record Literal(Object value) implements Expression {

    @Override
    public String name() {
      return "literal";
    }

    @Override
    public Object evaluate(Feature feature) {
      return value;
    }
  }

  record At(int index, Expression expression) implements Expression {

    @Override
    public String name() {
      return "at";
    }

    @Override
    public Object evaluate(Feature feature) {
      Object value = expression.evaluate(feature);
      if (value instanceof List list && index >= 0 && index < list.size()) {
        return list.get(index);
      }
      return null;
    }
  }

  record Get(String property) implements Expression {

    @Override
    public String name() {
      return "get";
    }

    @Override
    public Object evaluate(Feature feature) {
      return feature.getTags().get(property);
    }
  }

  record Has(String property) implements Expression<Boolean> {

    @Override
    public String name() {
      return "has";
    }

    @Override
    public Boolean evaluate(Feature feature) {
      return feature.getTags().get(property) != null;
    }
  }

  record In(Object value, Expression expression) implements Expression<Boolean> {

    @Override
    public String name() {
      return "in";
    }

    @Override
    public Boolean evaluate(Feature feature) {
      var expressionValue = expression.evaluate(feature);
      if (expressionValue instanceof List list) {
        return list.contains(value);
      } else if (expressionValue instanceof String string) {
        return string.contains(value.toString());
      } else {
        return false;
      }
    }
  }

  record IndexOf(Object value, Expression expression) implements Expression<Integer> {

    @Override
    public String name() {
      return "index-of";
    }

    @Override
    public Integer evaluate(Feature feature) {
      var expressionValue = expression.evaluate(feature);
      if (expressionValue instanceof List list) {
        return list.indexOf(value);
      } else if (expressionValue instanceof String string) {
        return string.indexOf(value.toString());
      } else {
        return -1;
      }
    }
  }

  record Length(Expression expression) implements Expression<Integer> {

    @Override
    public String name() {
      return "length";
    }

    @Override
    public Integer evaluate(Feature feature) {
      Object value = expression.evaluate(feature);
      if (value instanceof String string) {
        return string.length();
      } else if (value instanceof List list) {
        return list.size();
      } else {
        return -1;
      }
    }
  }

  record Slice(Expression expression, Expression start, Expression end) implements Expression {

    public Slice(Expression expression, Expression start) {
      this(expression, start, null);
    }

    @Override
    public String name() {
      return "slice";
    }

    @Override
    public Object evaluate(Feature feature) {
      Object value = expression.evaluate(feature);
      var startIndex = (Integer) start.evaluate(feature);
      if (value instanceof String string) {
        var endIndex = end == null ? string.length() : (Integer) end.evaluate(feature);
        return string.substring(startIndex, endIndex);
      } else if (value instanceof List list) {
        var endIndex = end == null ? list.size() : (Integer) end.evaluate(feature);
        return list.subList(startIndex, endIndex);
      } else {
        return List.of();
      }
    }
  }

  record Not(Expression expression) implements Expression {

    @Override
    public String name() {
      return "!";
    }

    @Override
    public Object evaluate(Feature feature) {
      return !(boolean) expression.evaluate(feature);
    }
  }

  record NotEqual(Expression left, Expression right) implements Expression {

    @Override
    public String name() {
      return "!=";
    }

    @Override
    public Object evaluate(Feature feature) {
      return new Not(new Equal(left, right)).evaluate(feature);
    }
  }

  record Less(Expression left, Expression right) implements Expression<Boolean> {

    @Override
    public String name() {
      return "<";
    }

    @Override
    public Boolean evaluate(Feature feature) {
      return (double) left.evaluate(feature) < (double) right.evaluate(feature);
    }
  }

  record LessOrEqual(Expression left, Expression right) implements Expression {

    @Override
    public String name() {
      return "<=";
    }

    @Override
    public Object evaluate(Feature feature) {
      return (double) left.evaluate(feature) <= (double) right.evaluate(feature);
    }
  }

  record Equal(Expression left, Expression right) implements Expression {

    @Override
    public String name() {
      return "==";
    }

    @Override
    public Object evaluate(Feature feature) {
      return left.evaluate(feature).equals(right.evaluate(feature));
    }
  }

  record Greater(Expression left, Expression right) implements Expression<Boolean> {

    @Override
    public String name() {
      return ">";
    }

    @Override
    public Boolean evaluate(Feature feature) {
      return (double) left.evaluate(feature) > (double) right.evaluate(feature);
    }
  }

  record GreaterOrEqual(Expression left, Expression right) implements Expression<Boolean> {

    @Override
    public String name() {
      return ">=";
    }

    @Override
    public Boolean evaluate(Feature feature) {
      return (double) left.evaluate(feature) >= (double) right.evaluate(feature);
    }
  }

  record All(List<Expression> expressions) implements Expression {

    @Override
    public String name() {
      return "all";
    }

    @Override
    public Object evaluate(Feature feature) {
      return expressions.stream().allMatch(expression -> (boolean) expression.evaluate(feature));
    }
  }

  record Any(List<Expression> expressions) implements Expression {

    @Override
    public String name() {
      return "any";
    }

    @Override
    public Object evaluate(Feature feature) {
      return expressions.stream().anyMatch(expression -> (boolean) expression.evaluate(feature));
    }
  }

  record Case(Expression condition, Expression then, Expression otherwise) implements Expression {

    @Override
    public String name() {
      return "case";
    }

    @Override
    public Object evaluate(Feature feature) {
      if ((boolean) condition.evaluate(feature)) {
        return then.evaluate(feature);
      } else {
        return otherwise.evaluate(feature);
      }
    }
  }

  record Coalesce(List<Expression> expressions) implements Expression {

    @Override
    public String name() {
      return "coalesce";
    }

    @Override
    public Object evaluate(Feature feature) {
      for (Expression expression : expressions) {
        Object value = expression.evaluate(feature);
        if (value != null) {
          return value;
        }
      }
      return null;
    }
  }

  record Match(Expression input, List<Expression> cases,
      Expression fallback) implements Expression {

    @Override
    public String name() {
      return "match";
    }

    @Override
    public Object evaluate(Feature feature) {
      if (cases.size() % 2 != 0) {
        throw new IllegalArgumentException(
            "match expression must have an even number of arguments");
      }
      var inputValue = input.evaluate(feature);
      for (int i = 0; i < cases.size(); i += 2) {
        Expression condition = cases.get(i);
        Expression then = cases.get(i + 1);
        if (inputValue.equals(condition.evaluate(feature))) {
          return then.evaluate(feature);
        }
      }
      return fallback.evaluate(feature);
    }
  }

  record Within(Expression expression) implements Expression {

    @Override
    public String name() {
      return "within";
    }

    @Override
    public Object evaluate(Feature feature) {
      throw new UnsupportedOperationException("within expression is not supported");
    }
  }

  record GeometryType(Expression expression) implements Expression<String> {

    @Override
    public String name() {
      return "geometry-type";
    }

    @Override
    public String evaluate(Feature feature) {
      Object property = feature.getGeometry();
      if (property instanceof Point) {
        return "Point";
      } else if (property instanceof LineString) {
        return "LineString";
      } else if (property instanceof Polygon) {
        return "Polygon";
      } else if (property instanceof MultiPoint) {
        return "MultiPoint";
      } else if (property instanceof MultiLineString) {
        return "MultiLineString";
      } else if (property instanceof MultiPolygon) {
        return "MultiPolygon";
      } else if (property instanceof GeometryCollection) {
        return "GeometryCollection";
      } else {
        return "Unknown";
      }
    }
  }


  class ExpressionSerializer extends StdSerializer<Expression> {

    public ExpressionSerializer() {
      super(Expression.class);
    }

    @Override
    public void serialize(Expression expression, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {
      jsonGenerator.writeStartArray();
      jsonGenerator.writeString(expression.name());
      for (Field field : expression.getClass().getDeclaredFields()) {
        field.setAccessible(true);
        try {
          Object value = field.get(expression);
          if (value instanceof Expression subExpression) {
            serialize(subExpression, jsonGenerator, serializerProvider);
          } else {
            jsonGenerator.writeObject(value);
          }
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
      jsonGenerator.writeEndArray();
    }
  }

  class ExpressionDeserializer extends StdDeserializer<Expression> {

    protected ExpressionDeserializer() {
      super(Expression.class);
    }

    @Override
    public Expression deserialize(JsonParser jsonParser,
        DeserializationContext deserializationContext) throws IOException {
      JsonNode node = jsonParser.getCodec().readTree(jsonParser);
      return deserializeJsonNode(node);
    }

    public Expression deserializeJsonNode(JsonNode node) {
      return switch (node.getNodeType()) {
        case BOOLEAN -> new Literal(node.asBoolean());
        case NUMBER -> new Literal(node.asDouble());
        case STRING -> new Literal(node.asText());
        case ARRAY -> deserializeJsonArray(node);
        default -> throw new IllegalArgumentException("Unknown node type: " + node.getNodeType());
      };
    }

    public Expression deserializeJsonArray(JsonNode node) {
      var arrayList = new ArrayList<JsonNode>();
      node.elements().forEachRemaining(arrayList::add);
      return switch (arrayList.get(0).asText()) {
        case "literal" -> new Literal(arrayList.get(1).asText());
        case "get" -> new Get(arrayList.get(1).asText());
        case "has" -> new Has(arrayList.get(1).asText());
        case ">" -> new Greater(deserializeJsonNode(arrayList.get(1)),
            deserializeJsonNode(arrayList.get(2)));
        case ">=" -> new GreaterOrEqual(deserializeJsonNode(arrayList.get(1)),
            deserializeJsonNode(arrayList.get(2)));
        case "<" -> new Less(deserializeJsonNode(arrayList.get(1)),
            deserializeJsonNode(arrayList.get(2)));
        case "<=" -> new LessOrEqual(deserializeJsonNode(arrayList.get(1)),
            deserializeJsonNode(arrayList.get(2)));
        case "==" -> new Equal(deserializeJsonNode(arrayList.get(1)),
            deserializeJsonNode(arrayList.get(2)));
        case "!=" -> new NotEqual(deserializeJsonNode(arrayList.get(1)),
            deserializeJsonNode(arrayList.get(2)));
        case "!" -> new Not(deserializeJsonNode(arrayList.get(1)));
        case "all" -> new All(arrayList.stream().skip(1).map(this::deserializeJsonNode).toList());
        case "any" -> new Any(arrayList.stream().skip(1).map(this::deserializeJsonNode).toList());
        case "case" -> new Case(deserializeJsonNode(arrayList.get(1)),
            deserializeJsonNode(arrayList.get(2)),
            deserializeJsonNode(arrayList.get(3)));
        case "coalesce" -> new Coalesce(
            arrayList.stream().skip(1).map(this::deserializeJsonNode).toList());
        case "match" -> new Match(
            deserializeJsonNode(arrayList.get(1)), arrayList.subList(2, arrayList.size() - 1)
                .stream().map(this::deserializeJsonNode).toList(),
            deserializeJsonNode(arrayList.get(arrayList.size() - 1)));
        case "within" -> new Within(deserializeJsonNode(arrayList.get(1)));
        default -> throw new IllegalArgumentException(
            "Unknown expression: " + arrayList.get(0).asText());
      };
    }
  }

  static Expression read(String json) throws IOException {
    return createObjectMapper().readValue(new StringReader(json), Expression.class);
  }

  static String write(Expression expression) throws IOException {
    return createObjectMapper().writeValueAsString(expression);
  }

  static Predicate<Feature> asPredicate(Expression expression) {
    return row -> {
      var result = expression.evaluate(row);
      if (result instanceof Boolean booleanResult) {
        return booleanResult;
      }
      throw new IllegalArgumentException(
          "Expression does not evaluate to a boolean: " + expression);
    };
  }

  static ObjectMapper createObjectMapper() {
    var mapper = new ObjectMapper();
    mapper.registerModule(createModule());
    return mapper;
  }

  static Module createModule() {
    var simpleModule = new SimpleModule("SimpleModule", new Version(1, 0, 0, null));
    simpleModule.addSerializer(Expression.class, new ExpressionSerializer());
    simpleModule.addDeserializer(Expression.class, new ExpressionDeserializer());
    return simpleModule;
  }

}
