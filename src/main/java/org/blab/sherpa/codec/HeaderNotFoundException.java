package org.blab.sherpa.codec;

import lombok.Getter;

@Getter
public class HeaderNotFoundException extends CodecException {
  private final String headerName;

  public HeaderNotFoundException(String headerName) {
    super();

    this.headerName = headerName;
  }
}
