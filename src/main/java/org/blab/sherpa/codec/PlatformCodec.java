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
    log.debug("Decoding: {}.", in);

    try {
      var builder = Event.builder()
          .topic(in.getTopic().toString())
          .fields(decodePayload(in.getPayloadAsBytes()));

      in.getUserProperties()
          .asList()
          .forEach(p -> {
            switch (p.getName().toString()) {
              case "timestamp" -> builder
                  .timestamp(Long.parseLong(p.getValue().toString()));
              default -> builder
                  .header(p.getName().toString(), p.getValue().toString());
            }
          });

      return Mono.just(builder.build());
    } catch (Exception e) {
      log.error(e);
      return Mono.empty();
    }
  }

  private Map<String, Object> decodePayload(byte[] raw) {
    log.debug("Parsing json: {}.", new String(raw));

    var payload = new String(raw, StandardCharsets.UTF_8);
    var token = new TypeToken<Map<String, Object>>() {
    }.getType();

    return gson.fromJson(payload, token);
  }

  @Override
  public Publisher<Mqtt5Publish> encode(Message<?> in) {
    log.debug("Encoding: {}.", in);

    switch (in) {
      case ErrorMessage e -> {
        log.error("Unexpected exception from platform.", e.getPayload());
        return Mono.empty();
      }
      case Event event -> {
        var properties = Mqtt5UserProperties.builder();

        in.getHeaders().entrySet()
            .stream()
            .filter(e -> !e.getKey().startsWith("_"))
            .forEach(e -> properties.add(e.getKey(), e.getValue().toString()));

        return Mono.just(Mqtt5Publish.builder()
            .topic(event.getTopic())
            .payload(encodePayload(event.getPayload()))
            .userProperties(properties.build())
            .build());
      }
      default -> throw new RuntimeException("Unsupported message type.");
    }
  }

  private byte[] encodePayload(Object payload) {
    return gson.toJson(payload).getBytes(StandardCharsets.UTF_8);
  }
}
