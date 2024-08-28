package org.blab.sherpa.codec;

import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;

/**
 * Interface for both encode (serialize) and decode (deserialize) objects of
 * particular type.
 *
 * <p>
 * Mendatory header names begin with "HEADERS_" and must begin with an
 * underscore to avoid conflicts with internal {@link Message} headers.
 */
public interface Codec<T> {
  static final String HEADERS_TOPIC = "_topic";
  static final String HEADERS_TIMESTAMP = "_timestamp";

  /** Decode the object into {@link Message}. */
  Publisher<Message<?>> decode(T in);

  /** Encode {@link Message} into an object. */
  Publisher<T> encode(Message<?> in);
}
