package org.blab.sherpa.platform;

import lombok.Getter;
import reactor.core.publisher.Mono;

/**
 * {@link Command} for subscribing to the desired platform topic.
 */
@Getter
public class Subscribe<T> extends Command<T> {
  private final String topic;

  public Subscribe(String topic, Mono<T> cmd) {
    super(cmd);
    this.topic = topic;
  }
}
