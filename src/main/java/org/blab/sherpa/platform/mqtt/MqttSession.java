package org.blab.sherpa.platform.mqtt;

import io.netty.buffer.Unpooled;
import org.blab.sherpa.codec.Codec;
import org.blab.sherpa.platform.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.support.MutableMessageBuilder;
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
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
@Scope("prototype")
public class MqttSession implements Session {
  private final Mqtt5RxClient client;

  private Codec<Message<?>> codec;

  public MqttSession(@Value("${platform.mqtt.host}") String host, @Value("${platform.mqtt.port}") int port) {
    this.client = Mqtt5Client.builder()
      .serverHost(host)
      .serverPort(port)
      .automaticReconnect().applyAutomaticReconnect()
      .buildRx();
  }

  public void setCodec(@Qualifier("jsonCodec") Codec<Message<?>> codec) {
    this.codec = codec;
  }

  @Override
  public Connect<Boolean> connect() {
    return new Connect<>(RxJava2Adapter
      .singleToMono(client.connect())
      .map(c -> c.getReasonCode().equals(Mqtt5ConnAckReasonCode.SUCCESS)));
  }

  @Override
  public Listen<Message<?>> listen() {
    return new Listen<>(RxJava2Adapter
      .flowableToFlux(client.publishes(MqttGlobalPublishFilter.SUBSCRIBED))
      .flatMap(this::decode));
  }

  @Override
  public Poll<Message<?>> poll(String topic) {
    return new Poll<>(topic, RxJava2Adapter
      .flowableToFlux(client.subscribePublishes(Mqtt5Subscribe.builder().topicFilter(topic).build()))
      .flatMap(this::decode)
      .take(1)
      .single());
  }

  @Override
  public Subscribe<String> subscribe(String topic) {
    return new Subscribe<>(topic, RxJava2Adapter
      .singleToMono(client.subscribe(Mqtt5Subscribe.builder().topicFilter(topic).build()))
      .map(s -> topic));
  }

  @Override
  public Unsubscribe<String> unsubscribe(String topic) {
    return new Unsubscribe<>(topic, RxJava2Adapter
      .singleToMono(client.unsubscribe(Mqtt5Unsubscribe.builder().topicFilter(topic).build()))
      .map(s -> topic));
  }

  @Override
  public Publish<Message<?>> publish(Message<?> msg) {
    return new Publish<>(msg, RxJava2Adapter
      .flowableToFlux(client.publish(Flowable.fromPublisher(encode(msg))))
      .single()
      .map(p -> msg));
  }

  @Override
  public Close<Void> close() {
    return new Close<>(RxJava2Adapter.completableToMono(client.disconnect()));
  }

  private Publisher<Message<?>> decode(Mqtt5Publish event) {
    return codec.decode(Mono.just(Unpooled.copiedBuffer(event.getPayloadAsBytes())))
      .map(msg -> {
        var m = MutableMessageBuilder.fromMessage(msg)
          .setHeader("topic", event.getTopic().toString());

        event.getUserProperties().asList().stream()
          .filter(p -> p.getName().toString().equals("timestamp"))
          .findAny()
          .ifPresent(p -> m.setHeader("timestamp", Long.parseLong(p.getValue().toString())));

        return m.build();
      });
  }

  private Publisher<Mqtt5Publish> encode(Message<?> msg) {
    return codec.encode(Mono.just(msg))
      .map(payload -> Mqtt5Publish.builder()
        .topic(Objects.requireNonNull(msg.getHeaders().get("topic", String.class)))
        .retain(true)
        .userProperties()
        .add("timestamp", Objects.requireNonNull(msg.getHeaders().getTimestamp()).toString())
        .applyUserProperties()
        .payload(payload.array())
        .build()
      );
  }
}
