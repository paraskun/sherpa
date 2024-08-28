package org.blab.sherpa.flow;

import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;

/** Handles an I/O event flow. */
public interface Executor {
  /**
   * Handle given {@link Message} flow.
   *
   * @return {@link Message} flow for further processing.
   */
  Publisher<Message<?>> execute(Message<?> in);
}