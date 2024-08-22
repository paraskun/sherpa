package org.blab.sherpa.flow;

import org.reactivestreams.Publisher;
import io.netty.buffer.ByteBuf;

/**
 * Abstract pipeline builder.
 *
 * Adds protocol support at the message interpretation level.
 */
public interface Flow {
  /**
   * Defines message flow for specific protocol implementation.
   *
   * @param in inbound message stream.
   * @return outbound message stream.
   */
  Publisher<ByteBuf> create(Publisher<ByteBuf> in);
}
