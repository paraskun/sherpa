package org.blab.sherpa.codec;

import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;

public interface Codec<T> {
  static final String HEADERS_TOPIC = "_topic";
  static final String HEADERS_TIMESTAMP = "_timestamp";
  static final String HEADERS_DESCRIPTION = "_description";

  Publisher<Message<?>> decode(Publisher<T> in);

  Publisher<T> encode(Publisher<Message<?>> in);
}

