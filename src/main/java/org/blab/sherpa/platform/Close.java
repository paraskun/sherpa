package org.blab.sherpa.platform;

import org.reactivestreams.Publisher;

/**
 * {@link Command} to close all active operations on
 * the platform and disconnect.
 */
public class Close<T> extends Command<T> {
  public Close(Publisher<T> cmd) {
    super(cmd);
  }
}
