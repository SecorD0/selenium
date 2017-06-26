// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.remote.server;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.openqa.selenium.remote.CapabilityType.BROWSER_NAME;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.remote.BeanToJsonConverter;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class BeginSession implements CommandHandler {

  private final ActiveSessionFactory sessionFactory;
  private final ActiveSessions allSessions;

  public BeginSession(ActiveSessions allSessions) {
    this.allSessions = allSessions;
    this.sessionFactory = new ActiveSessionFactory();
  }

  @Override
  public void execute(HttpRequest req, HttpResponse resp) throws IOException {
    // Copy the capabilities to disk
    Path allCaps = Files.createTempFile("selenium", ".json");

    try {
      Map<String, Object> ossKeys = new HashMap<>();
      Map<String, Object> alwaysMatch = new HashMap<>();
      List<Map<String, Object>> firstMatch = new LinkedList<>();

      readCapabilities(allCaps, req, ossKeys, alwaysMatch, firstMatch);

      ActiveSession session = sessionFactory.createSession(
          allCaps,
          ossKeys,
          alwaysMatch,
          firstMatch);
      allSessions.put(session);

      Object toConvert;
      switch (session.getDownstreamDialect()) {
        case OSS:
          toConvert = ImmutableMap.of(
              "status", 0,
              "sessionId", session.getId().toString(),
              "value", session.getCapabilities());
          break;

        case W3C:
          toConvert = ImmutableMap.of(
              "value", ImmutableMap.of(
                  "sessionId", session.getId().toString(),
                  "capabilities", session.getCapabilities()));
          break;

        default:
          throw new SessionNotCreatedException(
              "Unrecognized downstream dialect: " + session.getDownstreamDialect());
      }

      byte[] payload = new BeanToJsonConverter().convert(toConvert).getBytes(UTF_8);

      resp.setStatus(HTTP_OK);
      resp.setHeader("Cache-Control", "no-cache");

      resp.setHeader("Content-Type", JSON_UTF_8.toString());
      resp.setHeader("Content-Length", String.valueOf(payload.length));

      resp.setContent(payload);
    } finally {
      Files.delete(allCaps);
    }
  }

  private void readCapabilities(
      Path allCaps,
      HttpRequest req,
      Map<String, Object> ossKeys,
      Map<String, Object> alwaysMatch,
      List<Map<String, Object>> firstMatch) throws IOException {

    Charset charset = Charsets.UTF_8;
    try {
      String contentType = req.getHeader(CONTENT_TYPE);
      if (contentType != null) {
        MediaType mediaType = MediaType.parse(contentType);
        charset = mediaType.charset().or(Charsets.UTF_8);
      }
    } catch (IllegalArgumentException ignored) {
      // Do nothing.
    }

    try (InputStream rawIn = new BufferedInputStream(req.consumeContentStream());
         Reader reader = new InputStreamReader(rawIn, charset);
         Writer writer = Files.newBufferedWriter(allCaps, UTF_8);
         Reader in = new TeeReader(reader, writer);
         JsonReader json = new JsonReader(in)) {
      json.beginObject();

      while (json.hasNext()) {
        String name = json.nextName();

        switch (name) {
          case "desiredCapabilities":
            if (!ossKeys.isEmpty()) {
              json.skipValue();
            } else {
              ossKeys.putAll(sparseCapabilities(json));
            }
            break;

          case "capabilities":
            json.beginObject();
            while (json.hasNext()) {
              String capabilityName = json.nextName();

              switch (capabilityName) {
                case "alwaysMatch":
                  alwaysMatch.putAll(sparseCapabilities(json));
                  break;

                case "desiredCapabilities":
                  ossKeys.putAll(sparseCapabilities(json));
                  break;

                case "firstMatch":
                  json.beginArray();
                  while (json.hasNext()) {
                    firstMatch.add(sparseCapabilities(json));
                  }
                  json.endArray();
                  break;

                default:
                  json.skipValue();
                  break;
              }
            }
            json.endObject();
            break;

          default:
            json.skipValue();
            break;
        }
      }
    }
  }

  private Map<String, Object> sparseCapabilities(JsonReader json) throws IOException {
    Map<String, Object> caps = new HashMap<>();

    json.beginObject();

    while (json.hasNext()) {
      String key = json.nextName();

      JsonToken token = json.peek();
      switch (token) {
        case NULL:
          json.skipValue();
          break;

        case STRING:
          if (BROWSER_NAME.equals(key)) {
            caps.put(key, json.nextString());
          } else {
            caps.put(key, "");
            json.skipValue();
          }
          break;

        default:
          caps.put(key, "");
          json.skipValue();
          break;
      }
    }

    json.endObject();

    return caps;
  }

}
