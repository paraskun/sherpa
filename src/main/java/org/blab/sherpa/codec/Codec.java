package org.blab.sherpa.codec;

import java.util.Map;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Flux;

public interface Codec<T, V> {
  Flux<V> decode(Publisher<T> in);

  Flux<T> encode(Publisher<V> in);

  record Record(ByteBuf payload, Map<String, Object> hints) {}
}

