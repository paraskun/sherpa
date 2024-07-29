package org.blab.sherpa.service;

import org.springframework.messaging.Message;

public interface Session<T> {
  Connect<Void> connect();

  Listen<Message<T>> listen();

  Poll<Message<T>> poll(String topic);

  Subscribe<String> subscribe(String topic);

  Unsubscribe<String> unsubscribe(String topic);

  Publish<Message<T>> publish(Message<T> event);

  Close<Void> close();
}
