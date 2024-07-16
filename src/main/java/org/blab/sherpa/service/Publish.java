package org.blab.sherpa.service;

import lombok.Getter;
import reactor.core.publisher.Mono;

@Getter
public class Publish extends Command<Event> {
  private final Event event;

  protected Publish(Event event, Mono<Event> cmd) {
    super(cmd);
    this.event = event;
  }
}
