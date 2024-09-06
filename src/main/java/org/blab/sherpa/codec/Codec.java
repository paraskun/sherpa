package org.blab.sherpa.codec;

import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;

/**
 * Interface for both encode (serialize) and decode (deserialize) objects of
 * particular type.
 *
 * Headers that begin with underscore should not be encoded.
 */
public interface Codec<T> {
  /** Decode the object into {@link Message}. */
  Publisher<Message<?>> decode(T in);

  /** Encode {@link Message} into an object. */
  Publisher<T> encode(Message<?> in);
}
