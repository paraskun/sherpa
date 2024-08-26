package org.blab.sherpa.codec;

import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;

/** Message encoding specification. */
public interface Codec<T> {
  static final String HEADERS_TOPIC = "_topic";
  static final String HEADERS_TIMESTAMP = "_timestamp";
  static final String HEADERS_DESCRIPTION = "_description";

  /**
   * Decode messages.
   *
   * <p>
   * Unsupported messages will be replaced with
   * {@link org.springframework.messaging.support.ErrorMessage}
   * with {@link CodecException} and available headers.
   *
   * @param in steam of serialized messages.
   * @return stream of {@link Message}s;
   */
  Publisher<Message<?>> decode(Publisher<T> in);

  /**
   * Encode messages.
   *
   * <p>
   * Messages assumed to be valid.
   *
   * @param in stream of {@link Message}s.
   * @return stream of serialized messages.
   */
  Publisher<T> encode(Publisher<Message<?>> in);
}
