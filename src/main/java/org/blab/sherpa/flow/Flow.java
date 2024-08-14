package org.blab.sherpa.flow;

import org.reactivestreams.Publisher;
import io.netty.buffer.ByteBuf;

public interface Flow {
  Publisher<ByteBuf> create(Publisher<ByteBuf> in);
}

