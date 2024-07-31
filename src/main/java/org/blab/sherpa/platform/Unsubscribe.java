package org.blab.sherpa.platform;

import lombok.Getter;
import reactor.core.publisher.Mono;

@Getter
public class Unsubscribe<T> extends Command<T> {
  private final String topic;

  protected Unsubscribe(String topic, Mono<T> cmd) {
    super(cmd);
    this.topic = topic;
  }
}
