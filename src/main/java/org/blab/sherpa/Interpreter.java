package org.blab.sherpa;

import reactor.core.publisher.Mono;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;

public interface Interpreter {
  Mono<Void> interpret(NettyInbound in, NettyOutbound out);
}
