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

package org.openqa.selenium.grid.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.json.JsonInput;
import org.openqa.selenium.json.TypeToken;
import org.openqa.selenium.remote.SessionId;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class NodeStatus {

  private final NodeId nodeId;
  private final URI externalUri;
  private final int maxSessionCount;
  private final Map<Capabilities, Integer> stereotypes;
  private final Set<Active> snapshot;
  private final String registrationSecret;

  public NodeStatus(
      NodeId nodeId,
      URI externalUri,
      int maxSessionCount,
      Map<Capabilities, Integer> stereotypes,
      Collection<Active> snapshot,
      String registrationSecret) {
    this.nodeId = Require.nonNull("Node id", nodeId);
    this.externalUri = Require.nonNull("URI", externalUri);
    this.maxSessionCount = Require.positive("Max session count", maxSessionCount);

    this.stereotypes = ImmutableMap.copyOf(Require.nonNull("Stereotypes", stereotypes));
    this.snapshot = ImmutableSet.copyOf(Require.nonNull("Snapshot", snapshot));
    this.registrationSecret = registrationSecret;
  }

  public boolean hasCapacity() {
    return !stereotypes.isEmpty();
  }

  public boolean hasCapacity(Capabilities caps) {
    return stereotypes.getOrDefault(caps, 0) > 0;
  }

  public NodeId getNodeId() {
    return nodeId;
  }

  public URI getUri() {
    return externalUri;
  }

  public int getMaxSessionCount() {
    return maxSessionCount;
  }

  public Map<Capabilities, Integer> getStereotypes() {
    return stereotypes;
  }

  public Set<Active> getCurrentSessions() {
    return snapshot;
  }

  public String getRegistrationSecret() {
    return registrationSecret;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NodeStatus)) {
      return false;
    }

    NodeStatus that = (NodeStatus) o;
    return Objects.equals(this.nodeId, that.nodeId) &&
           Objects.equals(this.externalUri, that.externalUri) &&
           this.maxSessionCount == that.maxSessionCount &&
           Objects.equals(this.stereotypes, that.stereotypes) &&
           Objects.equals(this.snapshot, that.snapshot) &&
           Objects.equals(this.registrationSecret, that.registrationSecret);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeId, externalUri, maxSessionCount, stereotypes, snapshot);
  }

  private Map<String, Object> toJson() {
    return new ImmutableMap.Builder<String, Object>()
        .put("id", nodeId)
        .put("uri", externalUri)
        .put("maxSessions", maxSessionCount)
        .put("stereotypes", asCapacity(stereotypes))
        .put("sessions", snapshot)
        .put("registrationSecret", Optional.ofNullable(registrationSecret))
        .build();
  }

  private List<Map<String, Object>> asCapacity(Map<Capabilities, Integer> toConvert) {
    ImmutableList.Builder<Map<String, Object>> toReturn = ImmutableList.builder();
    toConvert.forEach((caps, count) -> toReturn.add(ImmutableMap.of(
        "capabilities", caps,
        "count", count)));
    return toReturn.build();
  }

  public static NodeStatus fromJson(JsonInput input) {
    List<Active> sessions = null;
    NodeId nodeId = null;
    URI uri = null;
    int maxSessions = 0;
    Map<Capabilities, Integer> stereotypes = null;
    String registrationSecret = null;

    input.beginObject();
    while (input.hasNext()) {

      switch (input.nextName()) {
        case "id":
          nodeId = input.read(NodeId.class);
          break;

        case "maxSessions":
          maxSessions = input.read(Integer.class);
          break;

        case "registrationSecret":
          registrationSecret = input.nextString();
          break;

        case "sessions":
          sessions = input.read(new TypeToken<List<Active>>(){}.getType());
          break;

        case "stereotypes":
          CapabilityCount count = input.read(CapabilityCount.class);
          stereotypes = count.getCounts();
          break;

        case "uri":
          uri = input.read(URI.class);
          break;

        default:
          input.skipValue();
          break;
      }
    }
    input.endObject();

    return new NodeStatus(
      nodeId,
      uri,
      maxSessions,
      stereotypes,
      sessions,
      registrationSecret);
  }

  public static class Active {

    private final Capabilities stereotype;
    private final SessionId id;
    private final Capabilities currentCapabilities;
    private final Instant startTime;

    public Active(Capabilities stereotype, SessionId id, Capabilities currentCapabilities, Instant startTime) {
      this.stereotype = ImmutableCapabilities.copyOf(Require.nonNull("Stereotype", stereotype));
      this.id = Require.nonNull("Session id", id);
      this.currentCapabilities =
          ImmutableCapabilities.copyOf(Require.nonNull("Capabilities", currentCapabilities));
      this.startTime = Require.nonNull("Start time", startTime);
    }

    public SessionId getId() {
      return id;
    }

    public Capabilities getStereotype() {
      return stereotype;
    }

    public SessionId getSessionId() {
      return id;
    }

    public Capabilities getCurrentCapabilities() {
      return currentCapabilities;
    }

    public Instant getStartTime() {
      return startTime;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Active)) {
        return false;
      }
      Active that = (Active) o;
      return Objects.equals(this.getStereotype(), that.getStereotype()) &&
             Objects.equals(this.id, that.id) &&
             Objects.equals(this.getCurrentCapabilities(), that.getCurrentCapabilities()) &&
             Objects.equals(this.getStartTime(), that.getStartTime());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getStereotype(), getSessionId(), getCurrentCapabilities(), getStartTime());
    }

    private Map<String, Object> toJson() {
      return ImmutableMap.of(
        "sessionId", getSessionId(),
        "stereotype", getStereotype(),
        "currentCapabilities", getCurrentCapabilities(),
        "startTime", getStartTime());
    }

    private static Active fromJson(JsonInput input) {
      SessionId id = null;
      Capabilities stereotype = null;
      Capabilities current = null;
      Instant startTime = null;

      input.beginObject();
      while (input.hasNext()) {
        switch (input.nextName()) {
          case "currentCapabilities":
            current = input.read(Capabilities.class);
            break;

          case "sessionId":
            id = input.read(SessionId.class);
            break;

          case "startTime":
            startTime = input.read(Instant.class);
            break;

          case "stereotype":
            stereotype = input.read(Capabilities.class);
            break;
        }
      }
      input.endObject();

      return new Active(stereotype, id, current, startTime);
    }
  }
}
