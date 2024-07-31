package org.blab.sherpa;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;

@SpringBootApplication
@Configuration
public class Sherpa {
  @EventListener
  public void onReady(ApplicationReadyEvent event, @Value("${server.port}") Integer port) {
    DisposableServer server = TcpServer.create()
      .port(port)
      .doOnChannelInit((observer, channel, remote) -> {
        channel.pipeline();
      })
      .bindNow();

    server.onDispose().block();
  }
}
