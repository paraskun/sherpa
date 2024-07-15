package org.blab.sherpa.service;

public interface Session {
  Connect connect();

  Listen listen();

  Poll poll(String topic);

  Subscribe subscribe(String topic);

  Unsubscribe unsubscribe(String topic);

  Publish publish(Event event);

  Close close();
}
