package org.blab.sherpa;

import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import org.blab.sherpa.flow.Flow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import reactor.netty.tcp.TcpServer;

@SpringBootApplication
public class Sherpa {
  @Value("${server.port}")
  private int port;

  public static void main(String[] args) {
    SpringApplication.run(Sherpa.class, args);
  }

  @EventListener
  public void onReady(ApplicationReadyEvent event) {
    var server = TcpServer.create()
        .port(port)
        .doOnChannelInit((obs, cfg, addr) -> cfg.pipeline()
            .addLast(new DelimiterBasedFrameDecoder(4096, Delimiters.nulDelimiter())))
        .handle((in, out) -> out.send(event.getApplicationContext()
            .getBean(Flow.class)
            .create(in.receive()))
            .then())
        .bindNow();

    server.onDispose().block();
  }
}
