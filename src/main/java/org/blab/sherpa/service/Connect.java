package org.blab.sherpa.service;

import reactor.core.publisher.Mono;

public class Connect extends Command<Boolean> {
  protected Connect(Mono<Boolean> cmd) {
    super(cmd);
  }
}
