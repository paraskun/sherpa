package org.blab.sherpa;

import org.blab.sherpa.service.Event;
import org.blab.sherpa.service.SessionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class Sherpa {
  private final SessionFactory factory;

  public Sherpa(SessionFactory factory) {
    this.factory = factory;
  }

  public static void main(String[] args) {
    SpringApplication.run(Sherpa.class, args);
  }

  @EventListener(ApplicationReadyEvent.class)
  private void onReady() throws InterruptedException {
    var session = factory.createSession();

    ((Mono<Boolean>) session.connect().result()).log().subscribe();
    ((Flux<Event>) session.listen().result()).log().subscribe();
    ((Mono<String>) session.subscribe("test").result()).log().subscribe();

    Thread.currentThread().join();
  }
}
