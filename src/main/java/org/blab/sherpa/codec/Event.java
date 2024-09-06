package org.blab.sherpa.codec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageHeaderAccessor;

import lombok.NonNull;

/** Mutable {@link org.springframework.messaging.Message} implementation. */
public class Event extends HashMap<String, Object>
    implements org.springframework.messaging.Message<Map<String, Object>> {

  public static final String HDRS_TOPIC = "_topic";
  public static final String HDRS_TIMESTAMP = "timestamp";

  private final MessageHeaderAccessor headers = new MessageHeaderAccessor();

  private Event() {
  }

  /**
   * Headers copy.
   *
   * @return copy of {@link MessageHeaders}, leaving the current Message
   *         modifiable.
   */
  @Override
  public @NonNull MessageHeaders getHeaders() {
    return headers.toMessageHeaders();
  }

  /**
   * Payload copy.
   *
   * @return copy of payload, leaving current Message modifiable.
   */
  @Override
  public @NonNull Map<String, Object> getPayload() {
    return new HashMap<>(this);
  }

  public Optional<Object> getHeader(@NonNull String key) {
    return Optional.ofNullable(headers.getHeader(key));
  }

  public String getTopic() {
    return (String) headers.getHeader(HDRS_TOPIC);
  }

  public Long getTimestamp() {
    return (Long) headers.getHeader(HDRS_TIMESTAMP);
  }

  public void setHeader(@NonNull String key, @NonNull Object value) {
    headers.setHeader(key, value);
  }

  public void setHeaderIfAbsent(@NonNull String key, @NonNull Object value) {
    headers.setHeaderIfAbsent(key, value);
  }

  public static EventBuilder builder() {
    return new EventBuilder();
  }

  public static class EventBuilder {
    private final Event event;

    EventBuilder() {
      event = new Event();
    }

    public EventBuilder topic(@NonNull String topic) {
      event.setHeaderIfAbsent(HDRS_TOPIC, topic);
      return this;
    }

    public EventBuilder timestamp(@NonNull Long timestamp) {
      event.setHeaderIfAbsent(HDRS_TIMESTAMP, timestamp);
      return this;
    }

    public EventBuilder header(@NotNull String key, @NotNull Object value) {
      if (!key.equals(HDRS_TOPIC) && !key.equals(HDRS_TIMESTAMP))
        event.setHeaderIfAbsent(key, value);

      return this;
    }

    public EventBuilder headers(@NonNull Map<String, Object> hdrs) {
      hdrs.forEach(this::header);
      return this;
    }

    public EventBuilder field(@NotNull String key, @NotNull Object value) {
      event.putIfAbsent(key, value);
      return this;
    }

    public EventBuilder fields(@NonNull Map<String, Object> fields) {
      fields.forEach(this::field);
      return this;
    }

    public Event build() {
      if (event.getHeader(HDRS_TOPIC).isEmpty())
        throw new FieldMissedException(HDRS_TOPIC);

      if (event.getHeader(HDRS_TIMESTAMP).isEmpty())
        throw new FieldMissedException(HDRS_TIMESTAMP);

      return event;
    }
  }
}
