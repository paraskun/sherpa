package org.blab.sherpa.service;

import reactor.core.publisher.Flux;

public abstract class Connect extends Command<Long> {
  protected Connect(Flux<Long> cmd) {
    super(cmd);
  }
}
