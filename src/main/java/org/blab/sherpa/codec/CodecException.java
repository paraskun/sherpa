package org.blab.sherpa.codec;

import org.springframework.messaging.MessageHeaders;

import lombok.Getter;

/** Base encoding exception. */
@Getter
public class CodecException extends RuntimeException {
  private final MessageHeaders headers;

  public CodecException(MessageHeaders hdrs) {
    super();
    this.headers = hdrs;
  }

  public CodecException(MessageHeaders hdrs, Throwable cause) {
    super(cause);
    this.headers = hdrs;
  }
}
