package org.blab.sherpa.service;

public class SessionClosedException extends SessionException {
  public SessionClosedException() {
    super();
  }

  public SessionClosedException(Throwable cause) {
    super(cause);
  }
}
