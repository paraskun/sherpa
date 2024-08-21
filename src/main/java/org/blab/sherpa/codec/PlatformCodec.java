package org.blab.sherpa.codec;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PlatformCodec implements Codec<Mqtt5Publish> {
  private final Gson gson;

  @Override
  public Flux<Message<?>> decode(Publisher<Mqtt5Publish> in) {
    return Flux.from(in)
      .map(event -> {
        try {
          var builder = MessageBuilder
            .withPayload(gson.fromJson(
                  new String(event.getPayloadAsBytes(), StandardCharsets.UTF_8),
                  new TypeToken<Map<String, Object>>(){}.getType()
                  ))
            .setHeader(HEADERS_TOPIC, event.getTopic().toString());

          event.getUserProperties().asList().forEach(p -> {
            switch (p.getName().toString()) {
              case "timestamp" -> builder.setHeader(HEADERS_TIMESTAMP, p.getValue().toByteBuffer().getLong());
              case "description" -> builder.setHeader(HEADERS_DESCRIPTION, p.getValue().toString());
            }
          });

          return builder.build();
        } catch (Exception e) {
          return new ErrorMessage(e);
        }
      });
  }

  @Override
  public Flux<Mqtt5Publish> encode(Publisher<Message<?>> in) {
    return Flux.from(in)
      .map(msg -> {
        var properties = Mqtt5UserProperties.builder()
          .add("timestamp", (String) msg.getHeaders().getOrDefault(HEADERS_TIMESTAMP, msg.getHeaders().getTimestamp().toString()));

        if (msg.getHeaders().containsKey(HEADERS_DESCRIPTION))
          properties.add("description", msg.getHeaders().get(HEADERS_DESCRIPTION, String.class));

        return Mqtt5Publish.builder()
          .topic(msg.getHeaders().get(HEADERS_TOPIC, String.class))
          .payload(gson.toJson(msg.getPayload()).getBytes(StandardCharsets.UTF_8))
          .userProperties(properties.build())
          .build();
      });
  }
}

