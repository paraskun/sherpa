package org.blab.sherpa;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;

import org.blab.sherpa.codec.Codec;
import org.blab.sherpa.flow.Handler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import reactor.netty.tcp.TcpServer;

@SpringBootApplication
@SuppressWarnings("unchecked")
public class Sherpa {
  @Value("${server.port}")
  private int port;

  @Value("${server.frame.length}")
  private int frameLength;

  public static void main(String[] args) {
    SpringApplication.run(Sherpa.class, args);
  }

  @EventListener
  public void onReady(ApplicationReadyEvent event) {
    var server = TcpServer.create()
        .port(port)
        .doOnChannelInit((obs, cfg, addr) -> cfg.pipeline()
            .addLast(new DelimiterBasedFrameDecoder(
                frameLength,
                Delimiters.nulDelimiter())))
        .handle((in, out) -> {
          var codec = event.getApplicationContext()
              .getBean("legacyCodec", Codec.class);

          var handler = event.getApplicationContext()
              .getBean(Handler.class);

          return out
              .send(codec.encode(handler.handle(codec.decode(in.receive()))))
              .then();
        })
        .bindNow();

    server.onDispose().block();
  }
}
