package org.blab.sherpa.flow;

import org.blab.sherpa.codec.Codec;
import org.blab.sherpa.codec.LegacyCodec;
import org.blab.sherpa.platform.Session;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
public class LegacyFlow implements Flow, ApplicationContextAware {
  private Codec<Message<?>> codec;
  private ApplicationContext context;

  @Override
  public void setApplicationContext(ApplicationContext context) {
    this.context = context;
  }

  @Autowired
  public void setCodec(@Qualifier("legacyCodec") Codec<Message<?>> codec) {
    this.codec = codec;
  }

  @Override
  public Publisher<ByteBuf> create(Publisher<ByteBuf> in) {
    var session = context.getBean("mqttSession", Session.class);

    return codec.encode(Flux.merge(
      Mono.from(session.connect().get()).log()
        .timeout(Duration.ofSeconds(5))
        .flatMap(b -> Mono.empty()),
      Flux.from(session.listen().get()).log(),
      Flux.from(codec.decode(in))
        .flatMap(msg -> {
          if (msg instanceof ErrorMessage error)
            return mapError(error);
          else
            return mapRequest(msg, session);
        }).doOnCancel(() -> Mono.from(session.close().get()).then()).log()
    ));
  }

  private Publisher<Message<?>> mapRequest(Message<?> msg, Session session) {
    return switch (msg.getHeaders().get(LegacyCodec.HEADERS_METHOD, LegacyCodec.Method.class)) {
      case POLL -> session
        .poll(msg.getHeaders().get("topic", String.class))
        .get();
      case SUBSCRIBE -> Mono.from(session
        .subscribe(msg.getHeaders().get("topic", String.class))
        .get()).then(Mono.empty());
      case UNSUBSCRIBE -> Mono.from(session
        .unsubscribe(msg.getHeaders().get("topic", String.class))
        .get()).then(Mono.empty());
      case PUBLISH -> Mono.from(session
        .publish(msg).get()).then(Mono.empty());
    };
  }

  private Publisher<Message<?>> mapError(ErrorMessage msg) {
    return Mono.just(MessageBuilder.withPayload(Map.of("val", "error"))
      .setHeader(Codec.HEADERS_TOPIC, msg.getHeaders().containsKey("topic") ? msg.getHeaders().get("topic", String.class) : "error")
      .setHeader(LegacyCodec.HEADERS_DESCRIPTION, msg.getPayload().getMessage())
      .build());
  }
}

