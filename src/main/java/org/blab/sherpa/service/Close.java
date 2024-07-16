package org.blab.sherpa.service;

import reactor.core.publisher.Mono;

public class Close extends Command<Long> {
  protected Close(Mono<Long> cmd) {
    super(cmd);
  }
}
