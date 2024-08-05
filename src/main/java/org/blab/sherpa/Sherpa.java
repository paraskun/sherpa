package org.blab.sherpa;

import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import org.blab.sherpa.flow.Flow;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import reactor.netty.tcp.TcpServer;

@SpringBootApplication
public class Sherpa {
  public static void main(String[] args) {
    SpringApplication.run(Sherpa.class, args);
  }

  @EventListener
  public void onReady(ApplicationReadyEvent event) {
    var server = TcpServer.create()
      .port(20041)
      .doOnChannelInit((obs, cfg, addr) -> cfg.pipeline()
        .addLast(new DelimiterBasedFrameDecoder(2048, Delimiters.nulDelimiter()))
      )
      .handle((in, out) -> event.getApplicationContext()
        .getBean(Flow.class, in, out).get())
      .bindNow();

    server.onDispose().block();
  }
}
