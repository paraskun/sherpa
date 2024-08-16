package org.blab.sherpa.codec;

import java.util.Map;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

import io.netty.buffer.Unpooled;
import lombok.RequiredArgsConstructor;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

@SpringBootTest
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class JsonCodecTest {
  private final Codec<Message<?>> codec;

  public void decode_Ok() {
    var record = new Codec.Record(
        Unpooled.copiedBuffer("{\"value\": 12.0}".getBytes()),
        Map.of(
          "_topic", "test",
          "_timestamp", 123L,
          "_description", "This is description."
        )
    );

    StepVerifier.create(codec.decode(TestPublisher.<Codec.Record>create().next(record).complete()))
      .expectNextMatches(m -> {
        return 
          m.getHeaders().containsKey("_timestamp") &&
          m.getHeaders().containsKey("_topic") &&
          m.getHeaders().containsKey("_description") &&
          ((Map<String, Object>) m.getPayload()).containsKey("value");
      })
      .verifyComplete();
  }

  public void decode_TopicMissed_ErrorMessage() {
    var record = new Codec.Record(
        Unpooled.copiedBuffer("{\"value\": 12.0}".getBytes()),
        Map.of(
          "_timestamp", 123L,
          "_description", "This is description."
          )
        );


    StepVerifier.create(codec.decode(TestPublisher.<Codec.Record>create().next(record).complete()))
      .expectNextMatches(m -> {
        return m instanceof ErrorMessage;
      })
      .verifyComplete();
  }

  public void decode_TimestampMissed_ErrorMessage() {}

  public void decode_JsonParseException_ErrorMessage() {}

  public void encode_Ok() {}

  public void encode_PayloadEmtpy_EmptyJson() {}
}

