package org.blab.sherpa.service;

import lombok.RequiredArgsConstructor;
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;

@RequiredArgsConstructor
public class SynchronousSession implements Session {
  private final IMqttAsyncClient client;

  @Override
  public Connect connect() {
    return null;
  }

  @Override
  public Listen listen() {
    return null;
  }

  @Override
  public Poll poll(String topic) {
    return null;
  }

  @Override
  public Subscribe subscribe(String topic) {
    return null;
  }

  @Override
  public Unsubscribe unsubscribe(String topic) {
    return null;
  }

  @Override
  public Publish publish(Event event) {
    return null;
  }

  @Override
  public Close close() {
    return null;
  }
}
