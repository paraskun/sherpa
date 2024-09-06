package org.blab.sherpa.codec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.messaging.MessageHeaders;

import lombok.NonNull;

/** Mutable {@link org.springframework.messaging.Message} implementation. */
public class Event extends HashMap<String, Object>
    implements org.springframework.messaging.Message<Map<String, Object>> {

  public static final String HDRS_TOPIC = "_topic";
  public static final String HDRS_TIMESTAMP = "timestamp";

  private final Map<String, Object> headers = new HashMap<>();

  protected Event() {
  }

  /**
   * Headers copy.
   *
   * @return copy of {@link MessageHeaders}, leaving the current Message
   *         modifiable.
   */
  @Override
  public @NonNull MessageHeaders getHeaders() {
    return new MessageHeaders(headers);
  }

  public @NonNull Map<String, Object> getHeadersUnsafe() {
    return headers;
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
    return Optional.ofNullable(headers.get(key));
  }

  public @NonNull String getTopic() {
    return (String) headers.get(HDRS_TOPIC);
  }

  public @NonNull Long getTimestamp() {
    return (Long) headers.get(HDRS_TIMESTAMP);
  }

  public void setHeader(@NonNull String key, @NonNull Object value) {
    headers.put(key, value);
  }

  public void setHeaderIfAbsent(@NonNull String key, @NonNull Object value) {
    headers.putIfAbsent(key, value);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    protected Event event;

    Builder() {
      event = new Event();
    }

    public Builder topic(@NonNull String topic) {
      event.setHeaderIfAbsent(HDRS_TOPIC, topic);
      return this;
    }

    public Builder timestamp(@NonNull Long timestamp) {
      event.setHeaderIfAbsent(HDRS_TIMESTAMP, timestamp);
      return this;
    }

    public Builder header(@NonNull String key, @NonNull Object value) {
      if (!key.equals(HDRS_TOPIC) && !key.equals(HDRS_TIMESTAMP))
        event.setHeaderIfAbsent(key, value);

      return this;
    }

    public Builder headers(@NonNull Map<String, Object> hdrs) {
      hdrs.forEach(this::header);
      return this;
    }

    public Builder field(@NonNull String key, @NonNull Object value) {
      event.putIfAbsent(key, value);
      return this;
    }

    public Builder fields(@NonNull Map<String, Object> fields) {
      fields.forEach(this::field);
      return this;
    }

    public Event build() {
      if (event.getHeader(HDRS_TOPIC).isEmpty())
        throw new HeaderMissedException(event.getHeaders(), HDRS_TOPIC);

      if (event.getHeader(HDRS_TIMESTAMP).isEmpty())
        throw new HeaderMissedException(event.getHeaders(), HDRS_TIMESTAMP);

      return event;
    }
  }
}
