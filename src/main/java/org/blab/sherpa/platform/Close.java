package org.blab.sherpa.platform;

import reactor.core.publisher.Mono;

/**
 * {@link Command} to close all active operations on 
 * the platform and disconnect.
 */
public class Close<T> extends Command<T> {
  public Close(Mono<T> cmd) {
    super(cmd);
  }
}
