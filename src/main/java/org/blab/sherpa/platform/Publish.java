package org.blab.sherpa.platform;

import lombok.Getter;

import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;

/**
 * {@link Command} to publish a {@link Message} on the platform.
 */
@Getter
public class Publish<T> extends Command<T> {
  private final Message<?> msg;

  public Publish(Message<?> msg, Publisher<T> cmd) {
    super(cmd);
    this.msg = msg;
  }
}
