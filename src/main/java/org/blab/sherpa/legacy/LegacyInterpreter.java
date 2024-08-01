package org.blab.sherpa.legacy;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.blab.sherpa.Interpreter;
import org.blab.sherpa.platform.Session;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;

@Component
@Slf4j
public class LegacyInterpreter implements Interpreter, ApplicationContextAware {
  private ApplicationContext context;

  @Override
  public Mono<Void> interpret(NettyInbound in, NettyOutbound out) {
    AtomicReference<Session> session = new AtomicReference<>();

    return in.receive()
        .doOnSubscribe(s -> {
          log.info("Session created.");
          session.set(context.getBean(Session.class));
        })
        .doOnCancel(() -> {
          log.info("Session closed.");
          session.get().close().get().subscribe(new BaseSubscriber<>() {});
        })
        .then();
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    context = applicationContext;
  }
}
