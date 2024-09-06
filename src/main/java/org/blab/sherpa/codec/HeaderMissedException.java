package org.blab.sherpa.codec;

import org.springframework.messaging.MessageHeaders;

import lombok.Getter;

@Getter
public class HeaderMissedException extends CodecException {
  private final String headerName;

  public HeaderMissedException(MessageHeaders hdrs, String headerName) {
    super(hdrs);
    this.headerName = headerName;
  }
}
