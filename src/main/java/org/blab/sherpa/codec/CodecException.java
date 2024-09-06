package org.blab.sherpa.codec;

/** Base encoding exception. */
public class CodecException extends RuntimeException {
  public CodecException() {
    super();
  }

  public CodecException(Throwable cause) {
    super(cause);
  }

  public CodecException(String description) {
    super(description);
  }
}
