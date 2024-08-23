package org.blab.sherpa.platform.mqtt;

import lombok.extern.log4j.Log4j2;
import org.blab.sherpa.codec.Codec;
import org.blab.sherpa.platform.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5RxClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe;
import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.Mqtt5Unsubscribe;
import io.reactivex.Flowable;
import io.reactivex.Single;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.HashSet;

@Log4j2
@Service
@Scope("prototype")
public class MqttSession implements Session {
  private final Set<String> topics = new HashSet<>();

  private final Codec<Mqtt5Publish> codec;
  private final Mqtt5RxClient client;

  public MqttSession(
      Codec<Mqtt5Publish> codec,
      @Value("${platform.mqtt.host}") String host,
      @Value("${platform.mqtt.port}") int port) {
    this.codec = codec;
    this.client = Mqtt5Client.builder()
        .serverHost(host)
        .serverPort(port)
        .automaticReconnect().applyAutomaticReconnect()
        .buildRx();
  }

  @Override
  public Connect<Boolean> connect() {
    return new Connect<>(mono(client.connect())
        .onErrorMap(t -> new PlatformException(t))
        .map(r -> true));
  }

  @Override
  public Listen<Message<?>> listen() {
    var in = flux(client.publishes(MqttGlobalPublishFilter.SUBSCRIBED))
        .filter(p -> topics.contains(p.getTopic().toString()))
        .onErrorMap(t -> new PlatformException(t));

    return new Listen<Message<?>>(Flux.from(codec.decode(in)));
  }

  @Override
  public Poll<Message<?>> poll(String topic) {
    var in = flux(client.subscribePublishes(
        Mqtt5Subscribe.builder()
            .topicFilter(topic)
            .build()))
        .onErrorMap(t -> new PlatformException(t))
        .take(1)
        .single()
        .flatMap(msg -> {
          if (topics.contains(topic))
            return Mono.just(msg);

          return mono(client.unsubscribe(
              Mqtt5Unsubscribe.builder()
                  .topicFilter(topic)
                  .build())
              .map(r -> msg));
        });

    return new Poll<Message<?>>(topic, Mono.from(codec.decode(in)));
  }

  @Override
  public Subscribe<String> subscribe(String topic) {
    return new Subscribe<>(topic, mono(client.subscribe(
        Mqtt5Subscribe.builder()
            .topicFilter(topic)
            .build()))
        .map(s -> topic)
        .onErrorMap(t -> new PlatformException(t))
        .doOnSuccess(topics::add));
  }

  @Override
  public Unsubscribe<String> unsubscribe(String topic) {
    return new Unsubscribe<>(topic, mono(client.unsubscribe(
        Mqtt5Unsubscribe
            .builder()
            .topicFilter(topic)
            .build()))
        .map(s -> topic)
        .onErrorMap(t -> new PlatformException(t))
        .doOnSuccess(topics::remove));
  }

  @Override
  public Publish<Message<?>> publish(Message<?> msg) {
    var out = Flowable.fromPublisher(codec.encode(Mono.just(msg)));
    var in = flux(client.publish(out))
      .map(Mqtt5PublishResult::getPublish)
      .onErrorMap(t -> new PlatformException(t))
      .take(1)
      .single();

    return new Publish<>(msg, Mono.from(codec.decode(in)));
  }

  @Override
  public Close<Void> close() {
    return new Close<>(RxJava2Adapter.completableToMono(client.disconnect()));
  }

  private <T> Flux<T> flux(Flowable<T> source) {
    return RxJava2Adapter.flowableToFlux(source);
  }

  private <T> Mono<T> mono(Single<T> source) {
    return RxJava2Adapter.singleToMono(source);
  }
}
