package org.blab.sherpa.codec;

import org.springframework.messaging.MessageHeaders;

import lombok.Getter;

/**
 * {@link CodecException} thrown when specified LegacyCodec.HEADERS_METHOD is
 * unknown.
 */
@Getter
public class UnknownMethodException extends CodecException {
  private final String methodName;

  public UnknownMethodException(MessageHeaders hdrs, String methodName) {
    super(hdrs);
    this.methodName = methodName;
  }
}
