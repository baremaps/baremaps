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

package com.baremaps.postgres.copy;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.bytefish.pgbulkinsert.pgsql.handlers.BaseValueHandler;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Encodes a value as a Postgres {@code jsonb} field.
 *
 * <p>
 * A {@code String} handed to this handler is written through as raw JSON rather than quoted as a
 * JSON string, so callers pass documents they have already serialized. Quoting them again would
 * store the document as a JSON string literal instead of the object it represents.
 */
public class JsonbValueHandler extends BaseValueHandler<Object> {

  /** The version byte that opens the jsonb wire format. Only version 1 has ever been defined. */
  private static final int PROTOCOL_VERSION = 1;

  private static final ObjectMapper objectMapper = objectMapper();

  private static ObjectMapper objectMapper() {
    var mapper = new ObjectMapper();
    var module = new SimpleModule();
    module.addSerializer(String.class, new RawJsonSerializer());
    mapper.registerModule(module);
    return mapper;
  }

  /** Writes a string as the JSON it already is, rather than as a quoted JSON string. */
  static class RawJsonSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator generator, SerializerProvider serializers)
        throws IOException {
      generator.writeRawValue(value);
    }
  }

  private final int jsonbProtocolVersion;

  public JsonbValueHandler() {
    this(PROTOCOL_VERSION);
  }

  public JsonbValueHandler(int jsonbProtocolVersion) {
    this.jsonbProtocolVersion = jsonbProtocolVersion;
  }

  private static byte[] asJson(Object object) throws IOException {
    return objectMapper.writeValueAsString(object).getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected void internalHandle(DataOutputStream buffer, Object value) throws IOException {
    byte[] json = asJson(value);
    buffer.writeInt(json.length + 1);
    buffer.writeByte(jsonbProtocolVersion);
    buffer.write(json);
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * The length has to match what {@link #internalHandle} writes, version byte included. Only a
   * {@code CollectionValueHandler} asks for it, to size the elements of an array; the copy path
   * itself does not.
   */
  @Override
  public int getLength(Object value) {
    try {
      return asJson(value).length + 1;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
