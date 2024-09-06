package org.blab.sherpa.codec;

import org.blab.sherpa.codec.LegacyCodec.Method;

import lombok.NonNull;

public class LegacyEvent extends Event {
  public static final String HDRS_METHOD = "_method";

  protected LegacyEvent() {
    super();
  }

  public @NonNull Method getMethod() {
    return (Method) getHeader(HDRS_METHOD).get();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends Event.Builder {
    public Builder method(@NonNull Method method) {
      event.setHeaderIfAbsent(HDRS_METHOD, method);
      return this;
    }

    @Override
    public LegacyEvent build() {
      if (event.getHeader(HDRS_METHOD).isEmpty())
        throw new HeaderMissedException(event.getHeaders(), HDRS_METHOD);

      return (LegacyEvent) super.build();
    }
  }
}
