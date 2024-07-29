package org.blab.sherpa.service;

import lombok.Getter;
import reactor.core.publisher.Mono;

@Getter
public class Publish<T> extends Command<T> {
  private final T event;

  protected Publish(T event, Mono<T> cmd) {
    super(cmd);
    this.event = event;
  }
}
