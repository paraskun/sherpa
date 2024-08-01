package org.blab.sherpa.legacy;

import java.text.SimpleDateFormat;
import org.blab.sherpa.Encoder;
import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import reactor.core.publisher.Flux;

@Component
public class LegacyEncoder implements Encoder<Message<?>> {
  @Override
  public Flux<ByteBuf> encode(Publisher<Message<?>> input) {
    return Flux.from(input).map(this::encode);
  }

  private ByteBuf encode(Message<?> msg) {
    var builder = new StringBuilder("time:" + formatTimestamp(msg.getHeaders().getTimestamp()));

    if (msg.getHeaders().containsKey("topic"))
      builder.append(String.format("|name:%s", msg.getHeaders().get("topic", String.class)));

    if (msg.getHeaders().containsKey("description"))
      builder.append(String.format("|descr:%s", msg.getHeaders().get("description", String.class)));

    if (msg.getPayload() != null)
      builder.append("|val:" + msg.getPayload().toString());

    return Unpooled.copiedBuffer(builder.toString().getBytes(CharsetUtil.US_ASCII));
  }

  private String formatTimestamp(Long timestamp) {
    return new SimpleDateFormat(LegacyDecoder.TIME_FMT).format(timestamp);
  }
}
