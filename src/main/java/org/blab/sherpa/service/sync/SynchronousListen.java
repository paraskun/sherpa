package org.blab.sherpa.service.sync;

import org.blab.sherpa.service.Event;
import org.blab.sherpa.service.Listen;
import reactor.core.publisher.Flux;

public class SynchronousListen extends Listen {
  protected SynchronousListen(Flux<Event> cmd) {
    super(cmd);
  }
}
