package org.blab.sherpa.codec;

import lombok.Getter;

@Getter
public class UnknownMethodException extends CodecException {
  private final String methodName;

  public UnknownMethodException(String methodName) {
    super();

    this.methodName = methodName;
  }
}
