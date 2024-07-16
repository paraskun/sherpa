package org.blab.sherpa.service;

import lombok.Getter;
import reactor.core.publisher.Mono;

@Getter
public class Unsubscribe extends Command<String> {
  private final String topic;

  protected Unsubscribe(String topic, Mono<String> cmd) {
    super(cmd);
    this.topic = topic;
  }
}
