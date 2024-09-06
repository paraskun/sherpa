package org.blab.sherpa;

import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import lombok.extern.log4j.Log4j2;

import java.util.Properties;

import org.blab.sherpa.codec.Codec;
import org.blab.sherpa.flow.Handler;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.tcp.TcpServer;

@Log4j2
@SpringBootApplication
@SuppressWarnings("unchecked")
public class Sherpa {
  public static final Integer DEFAULT_PORT = 8080;
  public static final Integer DEFAULT_FRAME_LENGTH = 2048;

  private Environment env;
  private ApplicationContext ctx;

  public static void main(String[] args) {
    SpringApplication.run(Sherpa.class, args);
  }

  @EventListener
  public void onReady(ApplicationReadyEvent event) {
    if (!env.containsProperty("server.port"))
      log.warn("Property server.port does not secified, default is used ({}).", DEFAULT_PORT);

    if (!env.containsProperty("server.frame.length"))
      log.warn("Property server.frame.length does not specified, default is used ({}).", DEFAULT_FRAME_LENGTH);

    var port = env.getProperty("server.port", Integer.class, DEFAULT_PORT);
    var length = env.getProperty("server.frame.length", Integer.class, DEFAULT_FRAME_LENGTH);

    var server = TcpServer.create()
        .port(port)
        .doOnChannelInit((obs, cfg, addr) -> cfg.pipeline()
            .addFirst(new DelimiterBasedFrameDecoder(length, Delimiters.lineDelimiter())))
        .handle(this::handle)
        .bindNow();

    server.onDispose().block();
  }

  private Publisher<Void> handle(NettyInbound in, NettyOutbound out) {
    var codec = (Codec<String>) ctx.getBean("legacyCodec", Codec.class);
    var handler = ctx.getBean("platformExecutor", Handler.class);

    return out.sendString(in.receive().asString()
        .flatMap(codec::decode)
        .flatMap(handler::handle)
        .flatMap(codec::encode))
        .then();

  }

  @Autowired
  public void setEnvironment(Environment env) {
    this.env = env;
  }

  @Autowired
  public void setContext(ApplicationContext ctx) {
    this.ctx = ctx;
  }
}
