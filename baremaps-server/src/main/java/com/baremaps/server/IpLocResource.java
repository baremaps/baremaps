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

package com.baremaps.server;

import static com.google.common.net.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;

import com.baremaps.iploc.IpLocObject;
import com.baremaps.iploc.IpLocRepository;
import com.google.common.net.InetAddresses;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.Param;
import com.linecorp.armeria.server.annotation.ProducesJson;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resource that provides access to the IP location database.
 */
public class IpLocResource {

  private static final Logger logger = LoggerFactory.getLogger(IpLocResource.class);

  private final IpLocRepository ipLocRepository;

  public IpLocResource(IpLocRepository ipLocRepository) {
    this.ipLocRepository = ipLocRepository;
  }

  public record IP(String ip) {
  }

  @Get("/api/ip")
  @ProducesJson
  public HttpResponse ip(
      ServiceRequestContext context,
      HttpHeaders requestHeaders,
      @Param("ip") String ip) {
    try {
      var address = InetAddresses.forString(
          Optional.ofNullable((CharSequence) ip)
              .or(() -> Optional.ofNullable(requestHeaders.get("X-Forwarded-For")))
              .or(() -> Optional.ofNullable(requestHeaders.get("X-Real-IP")))
              .orElse(context.remoteAddress().getAddress().getHostAddress())
              .toString().split(",")[0].trim());

      var responseHeaders = ResponseHeaders.builder(200)
          .add(CONTENT_TYPE, APPLICATION_JSON)
          .add(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
          .build();

      return HttpResponse.ofJson(responseHeaders, new IP(address.toString()));
    } catch (IllegalArgumentException e) {
      logger.error("Error while processing request", e);
      return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Get("/api/iploc")
  @ProducesJson
  public Response iploc(
      ServiceRequestContext context,
      HttpHeaders requestHeaders,
      @Param("ip") String ip) {
    try {
      var address = InetAddresses.forString(
          Optional.ofNullable((CharSequence) ip)
              .or(() -> Optional.ofNullable(requestHeaders.get("X-Forwarded-For")))
              .or(() -> Optional.ofNullable(requestHeaders.get("X-Real-IP")))
              .orElse(context.remoteAddress().getAddress().getHostAddress())
              .toString().split(",")[0].trim());
      List<IpLocObject> inetnumLocations = ipLocRepository.findByInetAddress(address);
      List<InetnumLocationDto> inetnumLocationDtos =
          inetnumLocations.stream().map(InetnumLocationDto::new).toList();
      var responseHeaders = ResponseHeaders.builder(200)
          .add(CONTENT_TYPE, APPLICATION_JSON)
          .add(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
          .build();
      return HttpResponse.ofJson(responseHeaders, inetnumLocationDtos);
    } catch (IllegalArgumentException e) {
      logger.error("Error while processing request", e);
      return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public record InetnumLocationDto(
      String geocoderInput,
      String inetStart,
      String inetEnd,
      double longitude,
      double latitude,
      String network,
      String country,
      String source,
      String precision) {

    public InetnumLocationDto(IpLocObject ipLocObject) {
      this(ipLocObject.geocoderInput(),
          ipLocObject.inetRange().start().toString().substring(1),
          ipLocObject.inetRange().end().toString().substring(1),
          ipLocObject.coordinate().getX(),
          ipLocObject.coordinate().getY(),
          ipLocObject.network(),
          ipLocObject.country(),
          ipLocObject.source(),
          ipLocObject.precision().toString());
    }
  }
}
