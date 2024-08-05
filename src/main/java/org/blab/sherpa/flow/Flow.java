package org.blab.sherpa.flow;

import reactor.core.publisher.Mono;

public interface Flow {
  Mono<Void> get();
}
