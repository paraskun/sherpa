package org.blab.sherpa.legacy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.blab.sherpa.Decoder;
import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.util.CharsetUtil;
import reactor.core.publisher.Flux;

@Component
public class LegacyDecoder implements Decoder<Message<?>> {
  public static final String TIME_FMT = "dd.MM.yyyy HH_mm_ss.SSS";

  @Override
  public Flux<Message<?>> decode(Publisher<ByteBuf> input) {
    return Flux.from(input).map(this::decode);
  }

  private Message<?> decode(ByteBuf buff) {
    var map = parse(buff.toString(CharsetUtil.US_ASCII));
    var builder = MessageBuilder.withPayload(map.get("val"));

    map.forEach((key, value) -> {
      switch (key.trim().toLowerCase()) {
        case "method", "meth", "m" -> builder.setHeader("method", filterMethod(value));
        case "name", "n" -> builder.setHeader("topic", filterTopic(value));
        case "descr" -> builder.setHeader("description", value);
        case "time" -> builder.setHeader("timestamp", filterTime(value));
      }
    });

    var msg = builder.build();

    if (!msg.getHeaders().containsKey("topic"))
      throw new DecoderException();

    if (!msg.getHeaders().containsKey("method"))
      throw new DecoderException();

    if (!msg.getHeaders().containsKey("timestamp"))
      msg.getHeaders().put("timestamp", System.currentTimeMillis());

    return msg;
  }

  private Map<String, String> parse(String msg) {
    return Arrays.stream(msg.split("\\|"))
        .map(s -> s.split(":"))
        .filter(s -> s.length == 2)
        .collect(Collectors.toMap(s -> s[0], s -> s[1]));
  }

  private String filterTopic(String topic) {
    return "vcas/" + topic;
  }

  private Long filterTime(String time) {
    if (time == null)
      return System.currentTimeMillis();

    try {
      return new SimpleDateFormat(TIME_FMT).parse(time).getTime();
    } catch (ParseException e) {
      return System.currentTimeMillis();
    }
  }

  private Method filterMethod(String method) {
    return switch (method) {
      case "get", "g", "getfull", "gf" -> Method.POLL;
      case "subscribe", "subscr", "sb" -> Method.SUBSCRIBE;
      case "release", "rel", "free", "f" -> Method.UNSUBSCRIBE;
      case "set", "s" -> Method.PUBLISH;
      default -> throw new DecoderException();
    };
  }
}
