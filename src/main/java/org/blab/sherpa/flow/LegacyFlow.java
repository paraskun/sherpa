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

@Service
public class LegacyFlow implements Flow, ApplicationContextAware {
  private ApplicationContext context;
  private Codec<ByteBuf> codec;

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

    session.connect().mono().log()
        .doOnError(e -> {
          session.close().mono().log().subscribe();
        })
        .subscribe();

    return codec.encode(Flux.merge(
        session.listen().flux(),
        interpret(codec.decode(in), session)));
  }

  private Publisher<Message<?>> interpret(Publisher<Message<?>> in, Session session) {
    return Flux.from(in)
        .flatMap(msg -> {
          if (msg instanceof ErrorMessage)
            return Mono.just(msg);

          return switch (msg.getHeaders().get(LegacyCodec.HEADERS_METHOD, LegacyCodec.Method.class)) {
            case POLL -> session
                .poll(msg.getHeaders().get(LegacyCodec.HEADERS_TOPIC, String.class))
                .mono()
                .onErrorComplete();
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
        });
  }
}
