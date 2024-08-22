package org.blab.sherpa.codec;

import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;

/**
 * Abstract protocol definition.
 *
 * Specifies message encoding according to a specific protocol.
 */
public interface Codec<T> {
  static final String HEADERS_TOPIC = "_topic";
  static final String HEADERS_TIMESTAMP = "_timestamp";
  static final String HEADERS_DESCRIPTION = "_description";

  /**
   * Decode given message stream according to the protocol definition.
   *
   * Produces {@link org.springframework.messaging.support.ErrorMessage} if 
   * given message cannot be decoded.
   *
   * @param in stream of pure messages.
   * @return stream of decoded {@link Message}s.
   */
  Publisher<Message<?>> decode(Publisher<T> in);

  /**
   * Encode given {@link Message} stream according to the protocol definition.
   *
   * Completes with exception if given {@link Message} cannot be encoded.
   *
   * @param in stream of {@link Message}s.
   * @return stream of message in desired format.
   */
  Publisher<T> encode(Publisher<Message<?>> in);
}
