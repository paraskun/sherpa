package org.blab.sherpa.service;

import org.reactivestreams.Publisher;

public abstract class Command<T> {
  private final Publisher<T> cmd;

  protected Command(Publisher<T> cmd) {
    this.cmd = cmd;
  }

  public Publisher<T> get() {
    return cmd;
  }
}
