package org.blab.sherpa.legacy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.blab.sherpa.Decoder;
import org.blab.sherpa.Encoder;
import org.blab.sherpa.Interpreter;
import org.blab.sherpa.platform.Session;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;

@Component
@RequiredArgsConstructor
public class LegacyInterpreter implements Interpreter, ApplicationContextAware {
  private final Decoder<Message<?>> decoder;
  private final Encoder<Message<?>> encoder;

  private ApplicationContext context;

  @Override
  public Mono<Void> interpret(NettyInbound in, NettyOutbound out) {
    var session = new AtomicReference<Session>();
    var output = new AtomicReference<FluxSink<Message<?>>>();
    var input = decoder.decode(in.receive());

    out.send(encoder.encode(Flux.create(sink -> {
      output.set(sink);
    })));

    return input
        .doOnSubscribe(s -> init(output, session))
        .doOnCancel(() -> close(session))
        .doOnNext(msg -> {
          switch (msg.getHeaders().get("method", Method.class)) {
            case Method.POLL -> Mono
                .from(session.get().poll(msg.getHeaders().get("topic", String.class)).get())
                .doOnSuccess(output.get()::next)
                .subscribe();
            case Method.PUBLISH -> Mono
                .from(session.get().publish(msg).get())
                .subscribe();
            case Method.SUBSCRIBE -> Mono
                .from(session.get().subscribe(msg.getHeaders().get("topic", String.class)).get())
                .subscribe();
            case Method.UNSUBSCRIBE -> Mono
                .from(session.get().unsubscribe(msg.getHeaders().get("topic", String.class)).get())
                .subscribe();
          }
        }).then();
  }

  private void init(AtomicReference<FluxSink<Message<?>>> output,
      AtomicReference<Session> session) {
    session.set(context.getBean(Session.class));

    Mono.from(session.get().connect().get())
        .log()
        .timeout(Duration.ofSeconds(15))
        .subscribe();

    Flux.from(session.get().listen().get())
        .log()
        .doOnNext(output.get()::next)
        .doOnCancel(output.get()::complete)
        .doOnError(output.get()::error);
  }

  private void close(AtomicReference<Session> session) {
    Mono.from(session.get().close().get())
        .log()
        .subscribe();
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    context = applicationContext;
  }
}
