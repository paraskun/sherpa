package org.blab.sherpa;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import reactor.netty.tcp.TcpServer;

@Configuration
@SpringBootApplication
public class Sherpa {
  @Value("${server.port}")
  private Integer port;

  public static void main(String[] args) {
    SpringApplication.run(Sherpa.class, args);
  }

  @EventListener
  public void onReady(ApplicationReadyEvent event) {
    var server = TcpServer.create()
        .port(port)
        .handle((in, out) -> null)
        .bindNow();

    server.onDispose().block();
  }
}
