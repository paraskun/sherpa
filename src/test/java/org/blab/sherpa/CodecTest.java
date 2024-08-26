package org.blab.sherpa;

import org.blab.sherpa.codec.Codec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.RequiredArgsConstructor;

@SpringBootTest
@RequiredArgsConstructor
public class CodecTest {
  private final Codec<?> codec;

  @Test
  public void decode_TopicMissed_ErrorMessage() {
  }

  @Test
  public void decode_TimestampMissed_CurrentTimestampUsed() {
  }

  @Test
  public void decode_PayloadMissed_NullPayload() {
  }

  @Test
  public void decode_SyntaxException_ErrorMessage() {
  }

  @Test
  public void encode_Malformed_RuntimeException() {
  }

  @Test
  public void encode_PayloadMissed_EmptyBody() {
  }
}
