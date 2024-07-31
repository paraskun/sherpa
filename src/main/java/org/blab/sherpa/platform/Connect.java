package org.blab.sherpa.platform;

import reactor.core.publisher.Mono;

public class Connect<T> extends Command<T> {
  protected Connect(Mono<T> cmd) {
    super(cmd);
  }
}
