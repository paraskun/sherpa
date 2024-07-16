package org.blab.sherpa.service;

import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5RxClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe;
import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.Mqtt5Unsubscribe;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.UUID;

@Log4j2
public class SynchronousSession implements Session {
  private final Mqtt5RxClient client;

  public SynchronousSession(InetSocketAddress address) {
    client =
        Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .automaticReconnect()
            .applyAutomaticReconnect()
            .serverAddress(address)
            .buildRx();
  }

  @Override
  public Connect connect() {
    return new Connect(RxJava2Adapter.singleToMono(client.connect()).map(c -> true));
  }

  @Override
  public Listen listen() {
    return new Listen(
        RxJava2Adapter.flowableToFlux(client.publishes(MqttGlobalPublishFilter.SUBSCRIBED))
            .map(p -> new Event(p.getTopic().toString(), p.getPayloadAsBytes())));
  }

  @Override
  public Poll poll(String topic) {
    return new Poll(
        topic,
        Mono.create(
            sink ->
                client
                    .subscribePublishes(Mqtt5Subscribe.builder().topicFilter(topic).build())
                    .doOnEach(
                        new BaseSubscriber<>() {
                          @Override
                          protected void hookOnNext(@NonNull Mqtt5Publish v) {
                            sink.success(new Event(v.getTopic().toString(), v.getPayloadAsBytes()));
                            cancel();
                          }
                        })
                    .subscribe()));
  }

  @Override
  public Subscribe subscribe(String topic) {
    return new Subscribe(
        topic,
        RxJava2Adapter.singleToMono(
            client.subscribe(Mqtt5Subscribe.builder().topicFilter(topic).build()).map(s -> topic)));
  }

  @Override
  public Unsubscribe unsubscribe(String topic) {
    return new Unsubscribe(
        topic,
        RxJava2Adapter.singleToMono(
            client
                .unsubscribe(Mqtt5Unsubscribe.builder().topicFilter(topic).build())
                .map(s -> topic)));
  }

  @Override
  public Publish publish(Event event) {
    var publisher =
        Flux.<Mqtt5Publish>generate(
            sink ->
                sink.next(
                    Mqtt5Publish.builder().topic(event.topic()).payload(event.payload()).build()));

    return new Publish(
        event,
        RxJava2Adapter.flowableToFlux(client.publish(RxJava2Adapter.fluxToFlowable(publisher)))
            .single()
            .map(p -> event));
  }

  @Override
  public Close close() {
    return new Close(
        RxJava2Adapter.completableToMono(client.disconnect()).then(Mono.fromCallable(() -> 0L)));
  }
}
