package org.blab.sherpa.platform.mqtt;

import lombok.extern.log4j.Log4j2;
import org.blab.sherpa.codec.Codec;
import org.blab.sherpa.platform.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5RxClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAckReasonCode;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe;
import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.Mqtt5Unsubscribe;
import io.reactivex.Flowable;
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
      @Value("${platform.mqtt.host}") String host, 
      @Value("${platform.mqtt.port}") int port,
      @Qualifier("platformCodec") Codec<Mqtt5Publish> codec
      ) {
    this.codec = codec;
    this.client = Mqtt5Client.builder()
      .serverHost(host)
      .serverPort(port)
      .automaticReconnect().applyAutomaticReconnect()
      .buildRx();
      }

  @Override
  public Connect<Boolean> connect() {
    return new Connect<>(RxJava2Adapter
        .singleToMono(client.connect())
        .map(c -> c.getReasonCode().equals(Mqtt5ConnAckReasonCode.SUCCESS)));
  }

  @Override
  public Listen<Message<?>> listen() {
    return new Listen<Message<?>>(
        Flux
        .from(codec
          .decode(RxJava2Adapter
            .flowableToFlux(client
              .publishes(MqttGlobalPublishFilter.SUBSCRIBED)))));
  }

  @Override
  public Poll<Message<?>> poll(String topic) {
    return new Poll<Message<?>>(topic, 
        Flux
        .from(codec
          .decode(RxJava2Adapter
            .flowableToFlux(client
              .subscribePublishes(Mqtt5Subscribe
                .builder()
                .topicFilter(topic)
                .build()))))
        .take(1)
        .single()
        .flatMap(msg -> {
          if (topics.contains(topic)) return Mono.just(msg);

          return RxJava2Adapter.singleToMono(client
              .unsubscribe(Mqtt5Unsubscribe.builder()
                .topicFilter(topic)
                .build())
              .map(r -> msg));
        }));
  }

  @Override
  public Subscribe<String> subscribe(String topic) {
    return new Subscribe<>(topic, RxJava2Adapter
        .singleToMono(client
          .subscribe(Mqtt5Subscribe
            .builder()
            .topicFilter(topic)
            .build()))
        .map(s -> topic)
        .doOnSuccess(topics::add));
  }

  @Override
  public Unsubscribe<String> unsubscribe(String topic) {
    return new Unsubscribe<>(topic, RxJava2Adapter
        .singleToMono(client
          .unsubscribe(Mqtt5Unsubscribe
            .builder()
            .topicFilter(topic)
            .build()))
        .map(s -> topic)
        .doOnSuccess(topics::remove));
  }

  @Override
  public Publish<Message<?>> publish(Message<?> msg) {
    return new Publish<>(msg, RxJava2Adapter
        .flowableToFlux(client
          .publish(Flowable
            .fromPublisher(codec.encode(Mono.just(msg)))))
        .single()
        .map(p -> msg));
  }

  @Override
  public Close<Void> close() {
    return new Close<>(RxJava2Adapter.completableToMono(client.disconnect()));
  }
}

