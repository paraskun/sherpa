package org.blab.sherpa.service;

import reactor.core.publisher.Flux;

public abstract class Listen extends Command<Event> {
  protected Listen(Flux<Event> cmd) {
    super(cmd);
  }
}
