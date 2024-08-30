package org.blab.sherpa.codec;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Log4j2
@Component
@RequiredArgsConstructor
public class PlatformCodec implements Codec<Mqtt5Publish> {
  private final Gson gson;

  @Override
  public Publisher<Message<?>> decode(Mqtt5Publish in) {
    try {
      var builder = MessageBuilder
          .withPayload(decodePayload(in.getPayloadAsBytes()))
          .setHeader(HEADERS_TOPIC, in.getTopic().toString());

      in.getUserProperties()
          .asList()
          .forEach(p -> {
            switch (p.getName().toString()) {
              case "timestamp" -> builder.setHeader(HEADERS_TIMESTAMP, p.getValue()
                  .toByteBuffer()
                  .getLong());
              default -> builder.setHeader("_" + p.getName().toString(), p.getValue()
                  .toString());
            }
          });

      return Mono.just(builder.build());
    } catch (Exception e) {
      log.warn(e);
      return Mono.empty();
    }
  }

  private Map<String, Object> decodePayload(byte[] raw) {
    var payload = new String(raw, StandardCharsets.UTF_8);
    var token = new TypeToken<Map<String, Object>>() {
    }.getType();

    return gson.fromJson(payload, token);
  }

  @Override
  public Publisher<Mqtt5Publish> encode(Message<?> in) {
    if (in instanceof ErrorMessage e) {
      log.warn("Unexpected error occured.", e.getPayload());
      return Mono.empty();
    }

    var properties = Mqtt5UserProperties.builder();

    in.getHeaders().entrySet()
        .stream()
        .filter(e -> e.getKey().startsWith("_"))
        .filter(e -> !e.getKey().equals(HEADERS_TOPIC))
        .forEach(e -> properties.add(e.getKey().substring(1), e.getValue().toString()));

    return Mono.just(Mqtt5Publish.builder()
        .topic(in.getHeaders().get(HEADERS_TOPIC, String.class))
        .payload(encodePayload(in.getPayload()))
        .userProperties(properties.build())
        .build());
  }

  private byte[] encodePayload(Object payload) {
    return gson.toJson(payload).getBytes(StandardCharsets.UTF_8);
  }
}
