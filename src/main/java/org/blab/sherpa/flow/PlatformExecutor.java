package org.blab.sherpa.flow;

import java.time.Duration;

import org.blab.sherpa.codec.LegacyCodec;
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
        .subscribe();
  }

  @Override
  public Publisher<Message<?>> handle(Message<?> msg) {
    if (msg instanceof ErrorMessage)
      return Mono.just(msg);

    return switch (msg.getHeaders().get(LegacyCodec.HEADERS_METHOD, Method.class)) {
      case Method.POLL -> session
          .poll(msg.getHeaders().get(LegacyCodec.HEADERS_TOPIC, String.class))
          .mono()
          .onErrorComplete();
      case Method.SUBSCRIBE -> session
          .subscribe(msg.getHeaders().get(LegacyCodec.HEADERS_TOPIC, String.class))
          .flux()
          .onErrorComplete();
      case Method.UNSUBSCRIBE -> session
          .unsubscribe(msg.getHeaders().get(LegacyCodec.HEADERS_TOPIC, String.class))
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
  }
}
