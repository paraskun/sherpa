package org.blab.sherpa.service;

import reactor.core.publisher.Flux;

public class Listen<T> extends Command<T> {
  protected Listen(Flux<T> cmd) {
    super(cmd);
  }
}
