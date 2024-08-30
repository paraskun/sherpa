package org.blab.sherpa.platform;

import org.reactivestreams.Publisher;

import lombok.Getter;

/**
 * {@link Command} for subscribing to the desired platform topic.
 */
@Getter
public class Subscribe<T> extends Command<T> {
  private final String topic;

  public Subscribe(String topic, Publisher<T> cmd) {
    super(cmd);
    this.topic = topic;
  }
}
