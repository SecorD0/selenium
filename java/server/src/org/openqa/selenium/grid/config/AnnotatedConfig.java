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

package org.openqa.selenium.grid.config;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A form of {@link Config} that is generated by looking at fields in the constructor arg that are
 * annotated with {@link ConfigValue}. The class hierarchy is not walked, null values are ignored,
 * and the order in which fields are read is not stable (meaning duplicate config values may give
 * different values each time).
 * <p>
 * The main use of this class is to allow an object configured using (for example) jcommander to be
 * used directly within the app, without requiring intermediate support classes to convert flags to
 * config values.
 */
public class AnnotatedConfig implements Config {

  private final Map<String, Map<String, String>> config;

  public AnnotatedConfig(Object obj) {
    Map<String, Map<String, String>> values = new HashMap<>();

    for (Field field : obj.getClass().getDeclaredFields()) {
      ConfigValue annotation = field.getAnnotation(ConfigValue.class);

      if (annotation == null) {
        continue;
      }

      if (Collection.class.isAssignableFrom(field.getType())) {
        throw new ConfigException("Collection fields may not be used for configuration: " + field);
      }

      if (Map.class.isAssignableFrom(field.getType())) {
        throw new ConfigException("Map fields may not be used for configuration: " + field);
      }

      field.setAccessible(true);
      Object value;
      try {
        value = field.get(obj);
      } catch (IllegalAccessException e) {
        throw new ConfigException("Unable to read field: " + field);
      }

      if (value == null) {
        continue;
      }

      Map<String, String> section = values.getOrDefault(annotation.section(), new HashMap<>());
      section.put(annotation.name(), String.valueOf(value));
      values.put(annotation.section(), section);
    }

    config = ImmutableMap.copyOf(values);
  }

  @Override
  public Optional<String> get(String section, String option) {
    Objects.requireNonNull(section, "Section name not set");
    Objects.requireNonNull(option, "Option name not set");

    Map<String, String> sec = config.get(section);
    if (sec == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(sec.get(option));
  }
}
