package org.blab.sherpa;

import org.reactivestreams.Publisher;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

public interface Decoder<T> {
  Flux<T> decode(Publisher<ByteBuf> in);
}
