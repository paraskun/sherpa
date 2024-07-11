package org.blab.sherpa.service.sync;

import org.blab.sherpa.service.Event;
import reactor.core.publisher.ConnectableFlux;

import java.net.InetSocketAddress;

public class SynchronousSessionTest {
  public static void main(String[] args) throws InterruptedException {
    var session = new SynchronousSession(new InetSocketAddress("localhost", 1883));

    ((ConnectableFlux<Event>) session.listen().result()).subscribe(System.out::println);

    Thread.currentThread().join();
  }
}
