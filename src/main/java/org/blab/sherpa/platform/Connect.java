package org.blab.sherpa.platform;

import org.reactivestreams.Publisher;

/**
 * {@link Command} to connect to the platform.
 */
public class Connect<T> extends Command<T> {
  public Connect(Publisher<T> cmd) {
    super(cmd);
  }
}
