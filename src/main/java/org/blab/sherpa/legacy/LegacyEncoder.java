package org.blab.sherpa.legacy;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import io.netty.util.CharsetUtil;
import reactor.core.publisher.Flux;

@Component
public class LegacyEncoder implements Encoder<Message<?>> {
  @Override
  public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
    return getEncodableMimeTypes().contains(mimeType);
  }

  @Override
  public Flux<DataBuffer> encode(Publisher<? extends Message<?>> inputStream,
      DataBufferFactory bufferFactory, ResolvableType elementType, MimeType mimeType,
      Map<String, Object> hints) {
    return Flux.from(inputStream).map(msg -> encode(msg, bufferFactory));
  }

  private DataBuffer encode(Message<?> msg, DataBufferFactory factory) {
    var builder = new StringBuilder("time:" + formatTimestamp(msg.getHeaders().getTimestamp()));

    if (msg.getHeaders().containsKey("topic"))
      builder.append(String.format("|name:%s", msg.getHeaders().get("topic", String.class)));

    if (msg.getHeaders().containsKey("description"))
      builder.append(String.format("|descr:%s", msg.getHeaders().get("description", String.class)));

    if (msg.getPayload() != null)
      builder.append("|val:" + msg.getPayload().toString());

    return factory.wrap(builder.toString().getBytes(CharsetUtil.US_ASCII));
  }

  private String formatTimestamp(Long timestamp) {
    return new SimpleDateFormat(LegacyDecoder.TIME_FMT).format(timestamp);
  }

  @Override
  public List<MimeType> getEncodableMimeTypes() {
    return List.of(MimeType.valueOf("application/octet-stream"));
  }
}
