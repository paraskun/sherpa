package org.blab.sherpa.flow;

import java.time.Duration;

import org.blab.sherpa.codec.LegacyEvent;
import org.blab.sherpa.codec.LegacyCodec.Method;
import org.blab.sherpa.platform.Session;
import org.reactivestreams.Publisher;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
@Scope("prototype")
public class PlatformExecutor implements Handler {
  private final Session session;

  public PlatformExecutor(Session session, Environment env) {
    var timeout = env.getProperty("platform.mqtt.timeout", Long.class, 15000L);

    this.session = session;

    session.connect().mono()
        .timeout(Duration.ofMillis(timeout))
        .log()
        .subscribe();
  }

  @Override
  public Publisher<Message<?>> handle(Message<?> msg) {
    return switch (msg) {
      case ErrorMessage e -> Mono.just(e);
      case LegacyEvent le ->
        switch (le.getMethod()) {
          case Method.POLL -> session
              .poll(le.getTopic())
              .mono()
              .onErrorComplete();
          case Method.SUBSCRIBE -> session
              .subscribe(le.getTopic())
              .flux()
              .onErrorComplete();
          case Method.UNSUBSCRIBE -> session
              .unsubscribe(le.getTopic())
              .mono()
              .onErrorComplete()
              .then(Mono.empty());
          case Method.PUBLISH -> session
              .publish(msg)
              .mono()
              .onErrorComplete()
              .then(Mono.empty());
          default -> Mono.empty();
        };

      default -> throw new RuntimeException("Unsupported message type.");
    };
  }
}
