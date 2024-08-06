package org.blab.sherpa.codec;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.EncodingException;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JsonCodec implements Codec<Message<?>> {
  private final Gson gson;

  @Override
  public Flux<Message<?>> decode(Publisher<ByteBuf> in) {
    return Flux.from(in)
      .map(buff -> {
          try {
            return MutableMessageBuilder
              .withPayload(gson.fromJson(
                buff.toString(StandardCharsets.UTF_8),
                new TypeToken<Map<String, Object>>() {
                }.getType()
              ))
              .build();
          } catch (Exception e) {
            throw new DecodingException(e.getMessage());
          }
        }
      );
  }

  @Override
  public Flux<ByteBuf> encode(Publisher<Message<?>> in) {
    return Flux.from(in)
      .map(msg -> {
        try {
          return Unpooled.
            copiedBuffer(gson.toJson(msg.getPayload())
              .getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
          throw new EncodingException(e.getMessage());
        }
      });
  }
}
