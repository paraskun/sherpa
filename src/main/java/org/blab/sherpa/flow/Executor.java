package org.blab.sherpa.flow;

import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;

/** Handles an I/O events. */
public interface Executor {
  /** Handle given {@link Message}. */
  Publisher<Message<?>> execute(Message<?> in);
}
