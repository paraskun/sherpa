package org.blab.sherpa.codec;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.ErrorMessage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@RequiredArgsConstructor
public class LegacyCodecTest {
  private final Codec<ByteBuf> codec;

  @Test
  public void decode_TopicMissed_HeaderNotFoundException() {
    var message = Unpooled.copiedBuffer("method:get", StandardCharsets.UTF_8);

    StepVerifier.create(codec.decode(Mono.just(message)))
        .expectNextMatches(msg -> msg instanceof ErrorMessage e &&
            e.getPayload() instanceof HeaderNotFoundException h &&
            h.getHeaderName().equals("topic"))
        .verifyComplete();
  }

  @Test
  public void decode_TimestampMissed_CurrentTimestampUsed() {
    var message = Unpooled.copiedBuffer("topic:test|method:get", StandardCharsets.UTF_8);

    StepVerifier.create(codec.decode(Mono.just(message)))
        .expectNextMatches(msg -> msg.getHeaders()
            .get(Codec.HEADERS_TOPIC, String.class)
            .equals("test") &&
            msg.getHeaders().containsKey(Codec.HEADERS_TIMESTAMP))
        .verifyComplete();
  }

  @Test
  public void decode_TimestampInWrongFormat_CurrentTimestampUsed() {
  }

  @Test
  public void decode_PayloadMissed_EmptyMap() {
  }

  @Test
  public void decode_MethodMissed_HeaderNotFoundException() {
  }

  @Test
  public void decode_UnsupportedMethod_UnknownMethodException() {
  }

  @Test
  public void encode_TopicMissed_HeaderNotFoundException() {
  }

  @Test
  public void encode_TimestampMissed_HeaderNotFoundException() {
  }

  @Test
  public void encode_PayloadMissed_EmptyBody() {
  }
}
