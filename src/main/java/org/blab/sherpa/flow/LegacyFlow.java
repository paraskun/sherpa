package org.blab.sherpa.flow;

import org.blab.sherpa.codec.Codec;
import org.blab.sherpa.codec.LegacyCodec;
import org.blab.sherpa.platform.Session;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Service;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class LegacyFlow implements Flow, ApplicationContextAware {
  private Codec<ByteBuf> codec;
  private ApplicationContext context;

  private @Value("${platform.mqtt.timeout}") int timeout;

  @Override
  public void setApplicationContext(ApplicationContext context) {
    this.context = context;
  }

  @Autowired
  public void setCodec(@Qualifier("legacyCodec") Codec<ByteBuf> codec) {
    this.codec = codec;
  }

  @Override
  public Publisher<ByteBuf> create(Publisher<ByteBuf> in) {
    var session = context.getBean("mqttSession", Session.class);

    return codec.encode(Flux.merge(
      connect(session),
      listen(session),
      interpret(in, session)
    ));
  }

  private Publisher<Message<?>> connect(Session session) {
    return Mono.from(session.connect().get())
      .timeout(Duration.ofSeconds(timeout))
      .flatMap(c -> Mono.<Message<?>>empty());
  }

  private Publisher<Message<?>> listen(Session session) {
    return Flux.from(session.listen().get());
  }

  private Publisher<Message<?>> interpret(Publisher<ByteBuf> msgs, Session session) {
    return Flux.from(codec.decode(msgs))
      .flatMap(msg -> {
        if (msg instanceof ErrorMessage) return Mono.just(msg);

        return switch (msg.getHeaders().get(LegacyCodec.HEADERS_METHOD, LegacyCodec.Method.class)) {
          case POLL -> session
            .poll(msg.getHeaders().get(LegacyCodec.HEADERS_TOPIC, String.class))
            .get();
          case SUBSCRIBE -> Mono.from(session
            .subscribe(msg.getHeaders().get(LegacyCodec.HEADERS_TOPIC, String.class))
            .get())
            .then(Mono.empty());
          case UNSUBSCRIBE -> Mono.from(session
            .unsubscribe(msg.getHeaders().get(LegacyCodec.HEADERS_TOPIC, String.class))
            .get())
            .then(Mono.empty());
          case PUBLISH -> Mono.from(session
            .publish(msg)
            .get())
            .then(Mono.empty());
        };
      }).log();
  }
}

