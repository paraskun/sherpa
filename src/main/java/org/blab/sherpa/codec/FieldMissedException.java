package org.blab.sherpa.codec;

import lombok.Getter;

@Getter
public class FieldMissedException extends CodecException {
  private final String fieldName;

  public FieldMissedException(String fieldName) {
    super();

    this.fieldName = fieldName;
  }
}
