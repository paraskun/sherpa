package org.blab.sherpa.service;

import reactor.core.publisher.Mono;

public class Close<T> extends Command<T> {
  protected Close(Mono<T> cmd) {
    super(cmd);
  }
}
