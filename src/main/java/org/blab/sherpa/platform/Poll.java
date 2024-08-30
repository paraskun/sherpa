package org.blab.sherpa.platform;

import org.reactivestreams.Publisher;

import lombok.Getter;

/**
 * {@link Command} to poll the latest event from the desired
 * platform topic.
 */
@Getter
public class Poll<T> extends Command<T> {
  private final String topic;

  public Poll(String topic, Publisher<T> cmd) {
    super(cmd);
    this.topic = topic;
  }
}
