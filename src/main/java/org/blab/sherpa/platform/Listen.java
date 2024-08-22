package org.blab.sherpa.platform;

import reactor.core.publisher.Flux;

/**
 * {@link Command} to listen for subscribed platform topics.
 */
public class Listen<T> extends Command<T> {
  public Listen(Flux<T> cmd) {
    super(cmd);
  }
}
