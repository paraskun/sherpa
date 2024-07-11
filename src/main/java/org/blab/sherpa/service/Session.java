package org.blab.sherpa.service;

public interface Session {
  Listen listen();

  Poll poll(String topic);

  Subscribe subscribe(String topic);

  Unsubscribe unsubscribe(String topic);

  Publish publish(Event event);

  void close();
}
