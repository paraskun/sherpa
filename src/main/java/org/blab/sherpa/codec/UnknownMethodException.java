package org.blab.sherpa.codec;

import lombok.Getter;

/**
 * {@link CodecException} thrown when specified LegacyCodec.HEADERS_METHOD is
 * unknown.
 */
@Getter
public class UnknownMethodException extends CodecException {
  private final String methodName;

  public UnknownMethodException(String methodName) {
    super();

    this.methodName = methodName;
  }
}
