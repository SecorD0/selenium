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

package org.openqa.selenium.grid.node.httpd;

import static org.openqa.selenium.grid.config.StandardGridRoles.NODE_ROLE;

import com.google.auto.service.AutoService;

import com.beust.jcommander.Parameter;

import org.openqa.selenium.grid.config.ConfigValue;
import org.openqa.selenium.grid.config.HasRoles;
import org.openqa.selenium.grid.config.NonSplittingSplitter;
import org.openqa.selenium.grid.config.Role;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
@AutoService(HasRoles.class)
public class NodeFlags implements HasRoles {

  @Parameter(
    names = "--max-sessions",
    description = "Maximum number of concurrent sessions.")
  @ConfigValue(section = "node", name = "max-concurrent-sessions", example = "8")
  public int maxSessions;

  @Parameter(
    names = {"--detect-drivers"}, arity = 1,
    description = "Autodetect which drivers are available on the current system, " +
                  "and add them to the Node. Defaults to true.")
  @ConfigValue(section = "node", name = "detect-drivers", example = "true")
  public Boolean autoconfigure;

  @Parameter(
    names = {"-I", "--driver-implementation"},
    description = "Drivers that should be checked. If specified, will skip autoconfiguration. " +
                  "Example: -I \"firefox\" -I \"chrome\"")
  @ConfigValue(section = "node", name = "drivers", example = "[\"firefox\", \"chrome\"]")
  public Set<String> driverNames = new HashSet<>();

  @Parameter(
    names = {"--driver-factory"},
    description = "Mapping of fully qualified class name to a browser configuration that this " +
                  "matches against. " +
                  "--driver-factory org.openqa.selenium.example.LynxDriverFactory " +
                  "'{\"browserName\": \"lynx\"}'",
    arity = 2,
    variableArity = true,
    splitter = NonSplittingSplitter.class)
  @ConfigValue(
    section = "node",
    name = "driver-factories",
    example = "[\"org.openqa.selenium.example.LynxDriverFactory '{\"browserName\": \"lynx\"}']")
  public List<String> driverFactory2Config;

  @Parameter(
    names = {"--public-url"},
    description = "Public URL of the Grid as a whole (typically the address of the Hub " +
                  "or the Router)")
  @ConfigValue(section = "node", name = "grid-url", example = "\"https://grid.example.com\"")
  public URL gridUri;

  @Parameter(
    names = {"--driver-configuration"},
    description = "List of configured drivers a Node supports. " +
                  "It is recommended to provide this type of configuration through a toml config " +
                  "file to improve readability." +
                  "--drivers-configuration name=\"Firefox Nightly\" max-sessions=2 " +
                  "stereotype='{\"browserName\": \"firefox\", \"browserVersion\": \"86\", " +
                  "\"moz:firefoxOptions\": " +
                  "{\"binary\":\"/Applications/Firefox Nightly.app/Contents/MacOS/firefox-bin\"}}'",
    arity = 3,
    variableArity = true,
    splitter = NonSplittingSplitter.class)
  @ConfigValue(
    section = "node",
    name = "driver-configuration",
    prefixed = true,
    example = "\n" +
              "name = \"Firefox Nightly\"\n" +
              "max-sessions = 2\n" +
              "stereotype = \"{\"browserName\": \"firefox\", \"browserVersion\": \"86\", " +
              "\"moz:firefoxOptions\": " +
              "{\"binary\":\"/Applications/Firefox Nightly.app/Contents/MacOS/firefox-bin\"}}\"")
  public List<String> driverConfiguration;

  @Parameter(
    names = "--register-cycle",
    description = "How often, in seconds, the Node will try to register itself for the first time to the Distributor.")
  @ConfigValue(section = "node", name = "register-cycle", example = "10")
  public int registerCycle;

  @Parameter(
    names = "--register-period",
    description = "How long, in seconds, will the Node try to register to the Distributor for " +
                  "the first time. After this period is completed, the Node will not attempt " +
                  "to register again.")
  @ConfigValue(section = "node", name = "register-period", example = "120")
  public int registerPeriod;

  @Parameter(
    names = "--heartbeat-period",
    description = "How often, in seconds, will the Node send heartbeat events to the Distributor " +
                  "to inform it that the Node is up.")
  @ConfigValue(section = "node", name = "heartbeat-period", example = "10")
  public int heartbeatPeriod;

  @Override
  public Set<Role> getRoles() {
    return Collections.singleton(NODE_ROLE);
  }
}
