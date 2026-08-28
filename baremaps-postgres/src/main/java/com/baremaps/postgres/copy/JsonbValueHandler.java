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

public class JsonbValueHandler extends BaseValueHandler<Object> {

  private static final ObjectMapper objectMapper;

  static {
    objectMapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addSerializer(String.class, new NoQuotesStringSerializer());
    objectMapper.registerModule(module);
  }

  static class NoQuotesStringSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeRawValue(value);
    }
  }

  private final int jsonbProtocolVersion;

  public JsonbValueHandler() {
    this(1);
  }

  public JsonbValueHandler(int jsonbProtocolVersion) {
    this.jsonbProtocolVersion = jsonbProtocolVersion;
  }

  private static byte[] asJson(Object object) throws IOException {
    try {
      String value = objectMapper.writeValueAsString(object);
      return value.getBytes("UTF-8");
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  protected void internalHandle(DataOutputStream buffer, Object value) throws IOException {
    byte[] utf8Bytes = asJson(value);
    buffer.writeInt(utf8Bytes.length + 1);
    buffer.writeByte(jsonbProtocolVersion);
    buffer.write(utf8Bytes);
  }

  @Override
  public int getLength(Object value) {
    try {
      byte[] utf8Bytes = asJson(value);
      return utf8Bytes.length;
    } catch (IOException e) {
      return 0;
    }
  }
}
