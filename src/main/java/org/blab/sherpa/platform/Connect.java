package org.blab.sherpa.platform;

import reactor.core.publisher.Mono;

/**
 * {@link Command} to connect to the platform.
 */
public class Connect<T> extends Command<T> {
  public Connect(Mono<T> cmd) {
    super(cmd);
  }
}
