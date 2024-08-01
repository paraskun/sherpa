package org.blab.sherpa.platform;

import org.springframework.context.annotation.Scope;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Scope("prototype")
public class MqttSession implements Session {
  @Override
  public Connect<Void> connect() {
    throw new UnsupportedOperationException("Unimplemented method 'connect'");
  }

  @Override
  public Listen<Message<?>> listen() {
    throw new UnsupportedOperationException("Unimplemented method 'listen'");
  }

  @Override
  public Poll<Message<?>> poll(String topic) {
    throw new UnsupportedOperationException("Unimplemented method 'poll'");
  }

  @Override
  public Subscribe<String> subscribe(String topic) {
    throw new UnsupportedOperationException("Unimplemented method 'subscribe'");
  }

  @Override
  public Unsubscribe<String> unsubscribe(String topic) {
    throw new UnsupportedOperationException("Unimplemented method 'unsubscribe'");
  }

  @Override
  public Publish<Message<?>> publish(Message<?> event) {
    throw new UnsupportedOperationException("Unimplemented method 'publish'");
  }

  @Override
  public Close<Void> close() {
    return new Close<>(Mono.just(null));
  }
}
