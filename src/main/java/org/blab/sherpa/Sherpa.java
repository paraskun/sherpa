package org.blab.sherpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import lombok.RequiredArgsConstructor;
import reactor.netty.tcp.TcpServer;

@SpringBootApplication
@RequiredArgsConstructor
public class Sherpa {
  private final Interpreter interpreter;

  public static void main(String[] args) {
    SpringApplication.run(Sherpa.class, args);
  }

  @EventListener
  public void onReady(ApplicationReadyEvent event) {
    var server = TcpServer.create()
        .port(20041)
        .handle((in, out) -> interpreter.interpret(in, out))
        .bindNow();

    server.onDispose().block();
  }
}
