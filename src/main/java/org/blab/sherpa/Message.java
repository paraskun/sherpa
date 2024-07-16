package org.blab.sherpa;

import lombok.Builder;

@Builder
public record Message(
    String topic, String value, String description, Method method, Long timestamp) {
  private static final String DESCRIPTION_FIELD_NAME_NOT_FOUND = "";
  private static final String DESCRIPTION_FIELD_METHOD_NOT_FOUND = "";
  private static final String DESCRIPTION_FIELD_VALUE_NOT_FOUND = "";
  private static final String DESCRIPTION_METHOD_NOT_SUPPORTED = "";

  enum Method {}
}
