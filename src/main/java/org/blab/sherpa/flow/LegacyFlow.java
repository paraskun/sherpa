package org.blab.sherpa.flow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blab.sherpa.codec.Codec;
import org.blab.sherpa.codec.LegacyCodec;
import org.blab.sherpa.platform.Session;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@Scope("prototype")
@RequiredArgsConstructor
public class LegacyFlow implements Flow {
  private final NettyInbound in;
  private final NettyOutbound out;

  private Session session;
  private Codec<Message<?>> codec;

  public void setSession(@Qualifier("mqttSession") Session session) {
    this.session = session;
  }

  public void setCodec(@Qualifier("legacyCodec") Codec<Message<?>> codec) {
    this.codec = codec;
  }

  @Override
  public Mono<Void> get() {
    Mono.from(session.connect().get())
      .log()
      .timeout(Duration.ofSeconds(15))
      .subscribe();

    return out.send(codec.encode(Flux.merge(
      Flux.from(session.listen().get()),
      Flux.from(codec.decode(in.receive()))
        .flatMap(msg -> {
          if (msg instanceof ErrorMessage error) return interpretErrorMessage(error);
          else return interpretMessage(msg);
        })))).then();
  }

  private Publisher<Message<?>> interpretMessage(Message<?> msg) {
    return switch (msg.getHeaders().get(LegacyCodec.METHOD, LegacyCodec.Method.class)) {
      case null -> throw new RuntimeException();
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

  private Publisher<Message<?>> interpretErrorMessage(ErrorMessage msg) {
    return Mono.just(MessageBuilder.withPayload(Map.of("val", "error"))
      .setHeader("topic", msg.getHeaders().containsKey("topic") ? msg.getHeaders().get("topic", String.class) : "error")
      .setHeader("description", msg.getPayload().getMessage())
      .build());
  }
}
