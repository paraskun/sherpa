package org.blab.sherpa.flow;

import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;

/** {@link Message} routing specification. */
public interface Router {
  /**
   *
   * @param in inbound message stream.
   * @return outbound message stream.
   */
  Publisher<Message<?>> route(Publisher<Message<?>> in);
}
