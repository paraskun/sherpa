package org.blab.sherpa.platform;

import lombok.Getter;
import reactor.core.publisher.Mono;

@Getter
public class Poll<T> extends Command<T> {
  private final String topic;

  public Poll(String topic, Mono<T> cmd) {
    super(cmd);
    this.topic = topic;
  }
}
