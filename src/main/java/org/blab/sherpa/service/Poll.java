package org.blab.sherpa.service;

import lombok.Getter;
import reactor.core.publisher.Mono;

@Getter
public class Poll extends Command<Event> {
  private final String topic;

  protected Poll(String topic, Mono<Event> cmd) {
    super(cmd);
    this.topic = topic;
  }
}
