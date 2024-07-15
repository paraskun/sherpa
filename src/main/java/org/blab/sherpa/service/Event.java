package org.blab.sherpa.service;

import java.util.Arrays;
import java.util.Objects;

public record Event(String topic, byte[] payload) {
  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (object == null || getClass() != object.getClass()) return false;
    Event event = (Event) object;
    return Objects.equals(topic, event.topic) && Objects.deepEquals(payload, event.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(topic, Arrays.hashCode(payload));
  }
}
