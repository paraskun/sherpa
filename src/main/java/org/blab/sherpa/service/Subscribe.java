package org.blab.sherpa.service;

import lombok.Getter;
import reactor.core.publisher.Mono;

@Getter
public abstract class Subscribe extends Command<String> {
  private final String topic;

  protected Subscribe(String topic, Mono<String> cmd) {
    super(cmd);
    this.topic = topic;
  }
}
