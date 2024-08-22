package org.blab.sherpa.platform;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A wrapper, responsible for a single operation on the
 * target platform (see "Command" design pattern for details).
 *
 * With durable nature in mind, Command stores its result with
 * the {@link Publisher} and will only be executed when the consumer
 * subscribes to the result.
 */
public abstract class Command<T> {
  private final Publisher<T> cmd;

  /**
   * Construct new command.
   *
   * @param cmd operation for which that Command responsible for.
   */
  protected Command(Publisher<T> cmd) {
    this.cmd = cmd;
  }

  /**
   * Retrive {@link Publisher}, which contains all the execution details.
   *
   * Please, note, that the underlying operation will only be
   * performed if the given {@link Publisher} is subscribed to.
   *
   * @return a {@link Publisher} that encapsulats execution details.
   */
  public Publisher<T> get() {
    return cmd;
  }

  public Mono<T> mono() {
    return Mono.from(cmd);
  }

  public Flux<T> flux() {
    return Flux.from(cmd);
  }
}
