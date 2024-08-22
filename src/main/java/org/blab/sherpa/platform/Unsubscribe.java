package org.blab.sherpa.platform;

import lombok.Getter;
import reactor.core.publisher.Mono;

/**
 * {@link Command} for unsubscribing from the desired platform topic.
 */
@Getter
public class Unsubscribe<T> extends Command<T> {
  private final String topic;

  public Unsubscribe(String topic, Mono<T> cmd) {
    super(cmd);
    this.topic = topic;
  }
}
