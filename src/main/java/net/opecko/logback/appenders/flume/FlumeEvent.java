/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package net.opecko.logback.appenders.flume;

import org.apache.flume.event.SimpleEvent;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.core.LogbackException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class FlumeEvent extends SimpleEvent implements ILoggingEvent {

  private static final String GUID = "guId";
  private static final String DEFAULT_MDC_PREFIX = "";
  private static final String DEFAULT_EVENT_PREFIX = "";
  private static final String TIMESTAMP = "timestamp";
  private final ILoggingEvent event;
  private final Map<String, String> ctx = new HashMap<String, String>();
  private final boolean compress;

  /**
   * Construct the FlumeEvent
   *
   * @param event The logback ILoggingEvent.
   * @param includes A comma separated list of MDC elements to include.
   * @param excludes A comma separated list of MDC elements to exclude.
   * @param required A comma separated list of MDC elements that are required to be defined.
   * @param mdcPrefix The value to prefix to MDC keys.
   * @param eventPrefix The value to prefix to event keys.
   * @param compress If true the event body should be compressed.
   */
  public FlumeEvent(final ILoggingEvent event,
      final String includes,
      final String excludes,
      final String required,
      String mdcPrefix,
      String eventPrefix,
      final boolean compress
  ) {
    this.event = event;
    this.compress = compress;
    final Map<String, String> headers = getHeaders();
    headers.put(TIMESTAMP, Long.toString(event.getTimeStamp()));
    if (mdcPrefix == null) {
      mdcPrefix = DEFAULT_MDC_PREFIX;
    }
    if (eventPrefix == null) {
      eventPrefix = DEFAULT_EVENT_PREFIX;
    }
    final Map<String, String> mdc = event.getMDCPropertyMap();
    if (includes != null) {
      final String[] array = includes.split(",");
      if (array.length > 0) {
        for (String str : array) {
          str = str.trim();
          if (mdc.containsKey(str)) {
            ctx.put(str, mdc.get(str));
          }
        }
      }
    } else if (excludes != null) {
      final String[] array = excludes.split(",");
      if (array.length > 0) {
        final List<String> list = new ArrayList<String>(array.length);
        for (final String value : array) {
          list.add(value.trim());
        }
        for (final Map.Entry<String, String> entry : mdc.entrySet()) {
          if (!list.contains(entry.getKey())) {
            ctx.put(entry.getKey(), entry.getValue());
          }
        }
      }
    } else {
      ctx.putAll(mdc);
    }

    if (required != null) {
      final String[] array = required.split(",");
      if (array.length > 0) {
        for (String str : array) {
          str = str.trim();
          if (!mdc.containsKey(str)) {
            throw new LogbackException("Required key " + str + " is missing from the MDC");
          }
        }
      }
    }

    headers.put(GUID, UUIDUtil.getTimeBasedUUID().toString());
    addContextData(mdcPrefix, headers, ctx);
  }

  protected void addContextData(
      final String prefix,
      final Map<String, String> fields,
      final Map<String, String> context
  ) {
    for (final Map.Entry<String, String> entry : context.entrySet()) {
      if (entry.getKey() != null && entry.getValue() != null) {
        fields.put(prefix + entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * Set the body in the event.
   * @param body The body to add to the event.
   */
  @Override
  public void setBody(final byte[] body) {
    if (body == null || body.length == 0) {
      super.setBody(new byte[0]);
      return;
    }
    if (compress) {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try {
        final GZIPOutputStream os = new GZIPOutputStream(baos);
        os.write(body);
        os.close();
      } catch (final IOException ioe) {
        throw new LogbackException("Unable to compress message", ioe);
      }
      super.setBody(baos.toByteArray());
    } else {
      super.setBody(body);
    }
  }

  public ILoggingEvent getEvent() {
    return event;
  }

  @Override
  public String getThreadName() {
    return event.getThreadName();
  }

  @Override
  public Level getLevel() {
    return event.getLevel();
  }

  @Override
  public String getMessage() {
    return event.getMessage();
  }

  @Override
  public Object[] getArgumentArray() {
    return event.getArgumentArray();
  }

  @Override
  public String getFormattedMessage() {
    return event.getFormattedMessage();
  }

  @Override
  public String getLoggerName() {
    return event.getLoggerName();
  }

  @Override
  public LoggerContextVO getLoggerContextVO() {
    return event.getLoggerContextVO();
  }

  @Override
  public IThrowableProxy getThrowableProxy() {
    return event.getThrowableProxy();
  }

  @Override
  public StackTraceElement[] getCallerData() {
    return event.getCallerData();
  }

  @Override
  public boolean hasCallerData() {
    return event.hasCallerData();
  }

  @Override
  public Marker getMarker() {
    return event.getMarker();
  }

  @Override
  public Map<String, String> getMDCPropertyMap() {
    return event.getMDCPropertyMap();
  }

  @SuppressWarnings("deprecation")
  @Override
  public Map<String, String> getMdc() {
    return event.getMdc();
  }

  @Override
  public long getTimeStamp() {
    return event.getTimeStamp();
  }

  @Override
  public void prepareForDeferredProcessing() {
    event.prepareForDeferredProcessing();
  }

}
