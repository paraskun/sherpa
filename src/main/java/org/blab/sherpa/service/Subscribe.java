package org.blab.sherpa.service;

import lombok.Getter;
import reactor.core.publisher.Mono;

@Getter
public class Subscribe<T> extends Command<T> {
  private final String topic;

  protected Subscribe(String topic, Mono<T> cmd) {
    super(cmd);
    this.topic = topic;
  }
}
