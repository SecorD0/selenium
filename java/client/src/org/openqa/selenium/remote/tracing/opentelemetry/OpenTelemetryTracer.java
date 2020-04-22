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

package org.openqa.selenium.remote.tracing.opentelemetry;

import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import org.openqa.selenium.remote.tracing.Propagator;
import org.openqa.selenium.remote.tracing.TraceContext;

import java.util.Objects;

public class OpenTelemetryTracer implements org.openqa.selenium.remote.tracing.Tracer {
  private final Tracer tracer;
  private final OpenTelemetryPropagator telemetryFormat;

  public OpenTelemetryTracer(Tracer tracer, HttpTextFormat httpTextFormat) {
    this.tracer = Objects.requireNonNull(tracer, "Tracer to use must be set.");

    Objects.requireNonNull(httpTextFormat, "Formatter to use must be set.");
    this.telemetryFormat = new OpenTelemetryPropagator(tracer, httpTextFormat);
  }

  @Override
  public TraceContext getCurrentContext() {
    Span currentSpan = tracer.getCurrentSpan();
    return new OpenTelemetryContext(tracer, currentSpan.getContext());
  }

  @Override
  public Propagator getPropagator() {
    return telemetryFormat;
  }

  io.opentelemetry.trace.Tracer getTracer() {
    return tracer;
  }
}
