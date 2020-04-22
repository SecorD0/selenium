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

package org.openqa.selenium.grid.log;

import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.MultiSpanProcessor;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.trace.SpanContext;
import org.openqa.selenium.grid.config.Config;
import org.openqa.selenium.remote.tracing.Tracer;
import org.openqa.selenium.remote.tracing.opentelemetry.OpenTelemetryTracer;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class LoggingOptions {

  private static final Logger LOG = Logger.getLogger(LoggingOptions.class.getName());
  private static final String LOGGING_SECTION = "logging";

  // We obtain the underlying tracer instance from the singleton instance
  // that OpenTelemetry maintains. If we blindly grabbed the tracing provider
  // and configured it, then subsequent calls would add duplicate exporters.
  // To avoid this, stash the configured tracer on a static and weep for
  // humanity. This implies that we're never going to need to configure
  // tracing more than once for the entire JVM, so we're never going to be
  // adding unit tests for this.
  private static Tracer tracer;

  private final Config config;

  public LoggingOptions(Config config) {
    this.config = Objects.requireNonNull(config);
  }

  public boolean isUsingStructuredLogging() {
    return config.getBool(LOGGING_SECTION, "structured-logs").orElse(false);
  }

  public boolean isUsingPlainLogs() {
    return config.getBool(LOGGING_SECTION, "plain-logs").orElse(true);
  }

  public Tracer getTracer() {
    LOG.info("Using OpenTelemetry for tracing");

    if (tracer != null) {
      return tracer;
    }

    synchronized (LoggingOptions.class) {
      if (tracer == null) {
        tracer = createTracer();
      }
    }
    return tracer;
  }

  private Tracer createTracer() {
    LOG.info("Using OpenTelemetry for tracing");
    TracerSdkProvider tracerFactory = OpenTelemetrySdk.getTracerProvider();

    List<SpanProcessor> exporters = new LinkedList<>();
    exporters.add(SimpleSpansProcessor.newBuilder(new SpanExporter() {
      @Override
      public ResultCode export(List<SpanData> spans) {
        spans.forEach(span -> {
          LOG.fine(String.valueOf(span));
        });
        return ResultCode.SUCCESS;
      }

      @Override
      public void shutdown() {
        // no-op
      }
    }).build());

    // The Jaeger exporter doesn't yet have a `TracerFactoryProvider`, so we
    //shall look up the class using reflection, and beg for forgiveness
    // later.
    Optional<SpanExporter> maybeJaeger = JaegerTracing.findJaegerExporter();
    maybeJaeger.ifPresent(
      exporter -> exporters.add(SimpleSpansProcessor.newBuilder(exporter).build()));
    tracerFactory.addSpanProcessor(MultiSpanProcessor.create(exporters));

    io.opentelemetry.trace.Tracer otTracer = tracerFactory.get("default");
    HttpTextFormat<SpanContext> httpTextFormat = otTracer.getHttpTextFormat();
    return new OpenTelemetryTracer(otTracer, httpTextFormat);
  }

  public void configureLogging() {
    if (!config.getBool(LOGGING_SECTION, "enable").orElse(true)) {
      return;
    }

    // Remove all handlers from existing loggers
    LogManager logManager = LogManager.getLogManager();
    Enumeration<String> names = logManager.getLoggerNames();
    while (names.hasMoreElements()) {
      Logger logger = logManager.getLogger(names.nextElement());
      Arrays.stream(logger.getHandlers()).forEach(logger::removeHandler);
    }

    // Now configure the root logger, since everything should flow up to that
    Logger logger = logManager.getLogger("");

    if (isUsingPlainLogs()) {
      Handler handler = new FlushingHandler(System.out);
      handler.setFormatter(new TerseFormatter());
      logger.addHandler(handler);
    }

    if (isUsingStructuredLogging()) {
      Handler handler = new FlushingHandler(System.out);
      handler.setFormatter(new JsonFormatter());
      logger.addHandler(handler);
    }
  }
}
