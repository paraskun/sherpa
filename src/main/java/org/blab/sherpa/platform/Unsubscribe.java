package org.blab.sherpa.platform;

import org.reactivestreams.Publisher;

import lombok.Getter;

/**
 * {@link Command} for unsubscribing from the desired platform topic.
 */
@Getter
public class Unsubscribe<T> extends Command<T> {
  private final String topic;

  public Unsubscribe(String topic, Publisher<T> cmd) {
    super(cmd);
    this.topic = topic;
  }
}
