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

package org.openqa.selenium.devtools.v85;

import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.idealized.Domains;
import org.openqa.selenium.devtools.idealized.Events;
import org.openqa.selenium.devtools.idealized.Javascript;
import org.openqa.selenium.devtools.idealized.Network;
import org.openqa.selenium.devtools.idealized.log.Log;
import org.openqa.selenium.devtools.idealized.target.Target;
import org.openqa.selenium.internal.Require;

public class V85Domains implements Domains {

  private final DevTools devtools;

  public V85Domains(DevTools devtools) {
    this.devtools = Require.nonNull("DevTools", devtools);
  }

  @Override
  public Events<?> events() {
    return new V85Events(devtools, javascript());
  }

  @Override
  public Javascript<?, ?> javascript() {
    return new V85Javascript(devtools);
  }

  @Override
  public Network<?, ?> network() {
    return new V85Network(devtools);
  }

  @Override
  public Target target() {
    return new V85Target();
  }

  @Override
  public Log log() {
    return new V85Log();
  }

}
