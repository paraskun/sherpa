package org.blab.sherpa.platform;

import lombok.Getter;
import reactor.core.publisher.Mono;

/**
 * {@link Command} to poll the latest event from the desired
 * platform topic.
 */
@Getter
public class Poll<T> extends Command<T> {
  private final String topic;

  public Poll(String topic, Mono<T> cmd) {
    super(cmd);
    this.topic = topic;
  }
}
