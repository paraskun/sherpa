package org.blab.sherpa.service;

import org.reactivestreams.Publisher;

public abstract class Command<T> {
  private final Publisher<T> result;

  protected Command(Publisher<T> cmd) {
    this.result = cmd;
  }

  public Publisher<T> result() {
    return result;
  }
}
