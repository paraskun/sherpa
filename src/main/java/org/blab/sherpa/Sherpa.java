package org.blab.sherpa;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;

import org.blab.sherpa.codec.Codec;
import org.blab.sherpa.flow.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

import reactor.netty.tcp.TcpServer;

@SpringBootApplication
@SuppressWarnings("unchecked")
public class Sherpa {
  private @Value("${server.port}") int port;
  private @Value("${server.frame.length}") int frameLength;

  public static void main(String[] args) {
    SpringApplication.run(Sherpa.class, args);
  }

  @EventListener
  public void onReady(ApplicationReadyEvent event) {
    var server = TcpServer.create()
        .port(port)
        .doOnChannelInit((obs, cfg, addr) -> cfg.pipeline().addFirst(before()))
        .handle((in, out) -> {
          var codec = codec(event.getApplicationContext());
          var executor = executor(event.getApplicationContext());

          return out
              .send(in.receive()
                  .flatMap(codec::decode)
                  .flatMap(executor::execute)
                  .flatMap(codec::encode))
              .then();
        })
        .bindNow();

    server.onDispose().block();
  }

  private ChannelHandler[] before() {
    return new ChannelHandler[] {
        new DelimiterBasedFrameDecoder(frameLength, Delimiters.nulDelimiter())
    };
  }

  private Codec<ByteBuf> codec(ApplicationContext context) {
    return context.getBean(Codec.class, "legacyCodec");
  }

  private Executor executor(ApplicationContext context) {
    return context.getBean(Executor.class);
  }
}
