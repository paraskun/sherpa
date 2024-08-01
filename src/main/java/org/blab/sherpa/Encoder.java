package org.blab.sherpa;

import org.reactivestreams.Publisher;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

public interface Encoder<T> {
  Flux<ByteBuf> encode(Publisher<T> in);
}
