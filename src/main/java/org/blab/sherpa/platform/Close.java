package org.blab.sherpa.platform;

import reactor.core.publisher.Mono;

public class Close<T> extends Command<T> {
  public Close(Mono<T> cmd) {
    super(cmd);
  }
}
