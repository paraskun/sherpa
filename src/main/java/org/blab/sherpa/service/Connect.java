package org.blab.sherpa.service;

import reactor.core.publisher.Mono;

public class Connect<T> extends Command<T> {
  protected Connect(Mono<T> cmd) {
    super(cmd);
  }
}
