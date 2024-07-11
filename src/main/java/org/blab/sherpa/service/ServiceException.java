package org.blab.sherpa.service;

public class ServiceException extends RuntimeException {
  public ServiceException() {
    super();
  }

  public ServiceException(Throwable cause) {
    super(cause);
  }
}
