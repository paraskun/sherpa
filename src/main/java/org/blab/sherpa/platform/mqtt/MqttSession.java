package org.blab.sherpa.platform.mqtt;

import org.blab.sherpa.codec.Codec;
import org.blab.sherpa.platform.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5RxClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe;
import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.Mqtt5Unsubscribe;

import io.reactivex.Flowable;
import io.reactivex.Single;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Scope("prototype")
public class MqttSession implements Session {
  private final Set<String> listening = ConcurrentHashMap.newKeySet();
  private final Set<String> polling = ConcurrentHashMap.newKeySet();

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
    return new Connect<>(toMono(client.connect())
        .onErrorMap(t -> new PlatformException(t))
        .map(c -> true));
  }

  @Override
  public Poll<Message<?>> poll(String topic) {
    var sub = Mqtt5Subscribe.builder()
        .topicFilter(topic)
        .build();

    var in = toFlux(client.subscribePublishes(sub))
        .onErrorMap(e -> new PlatformException(e))
        .take(1)
        .single()
        .flatMap(m -> {
          if (listening.contains(topic))
            return Mono.just(m);

          return unsubscribe(topic)
              .mono()
              .map(b -> m);
        })
        .doOnTerminate(() -> polling.remove(topic));

    return new Poll<Message<?>>(topic, in.flatMapMany(codec::decode));
  }

  @Override
  public Subscribe<Message<?>> subscribe(String topic) {
    var sub = Mqtt5Subscribe.builder()
        .topicFilter(topic)
        .build();

    return new Subscribe<>(topic, toFlux(client.subscribePublishes(sub))
        .onErrorMap(t -> new PlatformException(t))
        .flatMap(codec::decode));
  }

  @Override
  public Unsubscribe<Boolean> unsubscribe(String topic) {
    var usub = Mqtt5Unsubscribe
        .builder()
        .topicFilter(topic)
        .build();

    return new Unsubscribe<>(topic, toMono(client.unsubscribe(usub))
        .onErrorMap(t -> new PlatformException(t))
        .map(s -> true)
        .doOnSuccess(b -> listening.remove(topic)));
  }

  @Override
  public Publish<Boolean> publish(Message<?> msg) {
    var out = Flowable.fromPublisher(codec.encode(msg));

    return new Publish<>(msg, toFlux(client.publish(out))
        .onErrorMap(t -> new PlatformException(t))
        .map(p -> true)
        .take(1)
        .single());
  }

  @Override
  public Close<Void> close() {
    return new Close<>(RxJava2Adapter.completableToMono(client.disconnect()));
  }

  private <T> Flux<T> toFlux(Flowable<T> source) {
    return RxJava2Adapter.flowableToFlux(source);
  }

  private <T> Mono<T> toMono(Single<T> source) {
    return RxJava2Adapter.singleToMono(source);
  }
}
