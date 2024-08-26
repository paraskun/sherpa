package org.blab.sherpa;

import org.blab.sherpa.platform.Session;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.RequiredArgsConstructor;

@SpringBootTest
@RequiredArgsConstructor
public class SessionTest {
  private final Session session;

  @Test
  public void connect_Success_CompleteWithTrue() {
  }

  @Test
  public void connect_ManuallyClosed_CompleteEmpty() {
  }

  @Test
  public void connect_Error_CompleteWithPlatformException() {
  }

  @Test
  public void listen_Success_MessageStream() {
  }

  @Test
  public void listen_ManuallyClosed_CompleteEmpty() {
  }

  @Test
  public void listen_Error_CompleteWithPlatformException() {
  }

  @Test
  public void listen_UnsupportedMessage_Skipped() {
  }

  @Test
  public void poll_Success_CompleteWithMessage() {
  }

  @Test
  public void poll_ManuallyClosed_CompleteEmpty() {
  }

  @Test
  public void poll_Error_CompleteWithPlatformException() {
  }

  @Test
  public void poll_UnsupportedMessage_Skipped() {
  }

  @Test
  public void poll_Subscribed_MessageDuplicatedToListen() {
  }

  @Test
  public void subscribe_Success_CompleteWithTopic() {
  }

  @Test
  public void subscribe_ManuallyClosed_CompleteEmpty() {
  }

  @Test
  public void subscribe_Error_CompleteWithPlatformException() {
  }

  @Test
  public void unsubscribe_Success_CompleteWithTopic() {
  }

  @Test
  public void unsubscribe_ManuallyClosed_CompleteEmpty() {
  }

  @Test
  public void unsubscribe_Polling_IgnoreAndCompleteWithTopic() {
  }

  @Test
  public void unsubscribe_Error_CompleteWithPlatformException() {
  }

  @Test
  public void publish_Success_CompleteWithMessage() {
  }

  @Test
  public void publish_ManuallyClosed_CompleteEmpty() {
  }

  @Test
  public void publish_Error_CompleteWithPlatformException() {
  }

  @Test
  public void publish_UnsupportedFormat_CompleteWithCodecException() {
  }
}
