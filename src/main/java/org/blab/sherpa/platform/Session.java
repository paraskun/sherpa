package org.blab.sherpa.platform;

import org.springframework.messaging.Message;

public interface Session {
  Connect<Boolean> connect();

  Listen<Message<?>> listen();

  Poll<Message<?>> poll(String topic);

  Subscribe<String> subscribe(String topic);

  Unsubscribe<String> unsubscribe(String topic);

  Publish<Message<?>> publish(Message<?> msg);

  Close<Void> close();
}
