package org.blab.sherpa.platform;

import reactor.core.publisher.Flux;

public class Listen<T> extends Command<T> {
  public Listen(Flux<T> cmd) {
    super(cmd);
  }
}
