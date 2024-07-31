package org.blab.sherpa.platform;

import org.springframework.messaging.Message;

public interface Session {
  Connect<Void> connect();

  Listen<Message<?>> listen();

  Poll<Message<?>> poll(String topic);

  Subscribe<String> subscribe(String topic);

  Unsubscribe<String> unsubscribe(String topic);

  Publish<Message<?>> publish(Message<?> event);

  Close<Void> close();
}
