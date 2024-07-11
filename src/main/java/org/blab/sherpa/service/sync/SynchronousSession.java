package org.blab.sherpa.service.sync;

import lombok.extern.log4j.Log4j2;
import org.blab.sherpa.service.*;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.net.InetSocketAddress;
import java.util.*;

@Log4j2
public class SynchronousSession implements Session {
  private final Queue<MonoSink<IMqttAsyncClient>> synchronizationQueue = new LinkedList<>();
  private final IMqttAsyncClient client;
  private final ConnectableFlux<Event> listener;

  public SynchronousSession(InetSocketAddress address) {
    try {
      client =
          new MqttAsyncClient(
              String.format("tcp://%s:%d", address.getHostName(), address.getPort()),
              UUID.randomUUID().toString(),
              new MemoryPersistence());

      listener =
          Flux.<Event>create(
                  sink -> {
                    client.setCallback(
                        new MqttCallback() {
                          @Override
                          public void connectComplete(boolean reconnect, String serverURI) {
                            log.debug("Connected: {}.", serverURI);

                            synchronized (client) {
                              while (!synchronizationQueue.isEmpty())
                                synchronizationQueue.poll().success(client);
                            }
                          }

                          @Override
                          public void disconnected(MqttDisconnectResponse disconnectResponse) {
                            log.debug("Disconnected: {}.", disconnectResponse);
                          }

                          @Override
                          public void mqttErrorOccurred(MqttException e) {
                            log.debug(e);
                            sink.error(e);
                            close();
                          }

                          @Override
                          public void messageArrived(String topic, MqttMessage message) {
                            sink.next(new Event(topic, message.getPayload()));
                          }

                          @Override
                          public void deliveryComplete(IMqttToken token) {
                            log.debug("Delivered: {}.", token);
                          }

                          @Override
                          public void authPacketArrived(int reasonCode, MqttProperties properties) {
                            log.debug("Packet arrived [{}]: {}.", reasonCode, properties);
                          }
                        });
                  })
              .log()
              .publish();

      listener.connect();
      client.connect(
          new MqttConnectionOptionsBuilder()
              .connectionTimeout(5)
              .automaticReconnect(true)
              .cleanStart(false)
              .build());
    } catch (MqttException e) {
      throw new ServiceException(e);
    }
  }

  protected Mono<IMqttAsyncClient> synchronize() {
    return Mono.create(
        sink -> {
          synchronized (client) {
            if (client.isConnected()) sink.success(client);
            else synchronizationQueue.offer(sink);
          }
        });
  }

  @Override
  public Listen listen() {
    return new SynchronousListen();
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
  public void close() {
    try {
      synchronized (client) {
        while (!synchronizationQueue.isEmpty())
          synchronizationQueue.poll().error(new InterruptedException());
      }

      client.close();
    } catch (MqttException e) {
      log.debug(e);
    }
  }

  protected class SynchronousListen extends Listen {
    protected SynchronousListen() {
      super(listener);
    }
  }

  protected class SynchronousSubscribe extends Subscribe {
    protected SynchronousSubscribe(String topic) {
      super(
          topic,
          Mono.create(
              sink -> {
                synchronize()
                    .doOnError(sink::error)
                    .doOnSuccess(
                        client -> {
                          try {
                            client.subscribe(
                                topic,
                                1,
                                null,
                                new MqttActionListener() {
                                  @Override
                                  public void onSuccess(IMqttToken asyncActionToken) {
                                    sink.success(topic);
                                  }

                                  @Override
                                  public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                                    sink.error(e);
                                  }
                                });
                          } catch (MqttException e) {
                            sink.error(e);
                          }
                        })
                    .subscribe();
              }));
    }
  }
}
