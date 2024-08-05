package org.blab.sherpa.platform;

import lombok.Getter;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;

@Getter
public class Publish<T> extends Command<T> {
  private final Message<?> msg;

  public Publish(Message<?> msg, Mono<T> cmd) {
    super(cmd);
    this.msg = msg;
  }
}
