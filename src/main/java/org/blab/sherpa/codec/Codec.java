package org.blab.sherpa.codec;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public interface Codec<T> {
  static final String HEADERS_TOPIC = "_topic";
  static final String HEADERS_TIMESTAMP = "_timestamp";

  Flux<T> decode(Publisher<ByteBuf> in);

  Flux<ByteBuf> encode(Publisher<T> in);
}

