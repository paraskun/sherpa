package org.blab.sherpa.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LegacyCodec implements Codec<Message<?>> {
  private static final String TIME_FMT = "dd.MM.yyyy HH_mm_ss.SSS";
  
  public static final String METHOD = "method";

  @Override
  public Flux<Message<?>> decode(Publisher<ByteBuf> in) {
    return Flux.from(in).map(buff -> {
      var fields = parse(buff.toString(StandardCharsets.UTF_8));

      var payload = new HashMap<String, Object>();
      var headers = new HashMap<String, Object>();

      try {
        fields.forEach((key, value) -> {
          switch (key.trim().toLowerCase()) {
            case "method", "meth", "m" -> headers.put(METHOD, decodeMethod(value));
            case "name", "n" -> headers.put("topic", decodeTopic(value));
            case "descr" -> headers.put("description", value);
            case "time" -> headers.put("timestamp", decodeTimestamp(value));
            default -> payload.put(key, value);
          }
        });
      } catch (DecoderException e) {
        return new ErrorMessage(e, new MessageHeaders(headers));
      }

      if (!headers.containsKey("topic"))
        return new ErrorMessage(new DecoderException("name field not found"), new MessageHeaders(headers));

      if (!headers.containsKey(METHOD))
        return new ErrorMessage(new DecoderException("method field not found"), new MessageHeaders(headers));

      return MessageBuilder.createMessage(payload, new MessageHeaders(headers));
    });
  }

  private Map<String, String> parse(String msg) {
    return Arrays.stream(msg.trim().split("\\|"))
      .map(s -> s.split(":"))
      .filter(s -> s.length == 2)
      .collect(Collectors.toMap(s -> s[0], s -> s[1]));
  }

  private String decodeTopic(String topic) {
    return "vcas/" + topic;
  }

  private Long decodeTimestamp(String time) {
    try {
      return new SimpleDateFormat(TIME_FMT).parse(time).getTime();
    } catch (Exception e) {
      return null;
    }
  }

  private Method decodeMethod(String method) {
    return switch (method) {
      case "get", "g", "getfull", "gf" -> Method.POLL;
      case "subscribe", "subscr", "sb" -> Method.SUBSCRIBE;
      case "release", "rel", "free", "f" -> Method.UNSUBSCRIBE;
      case "set", "s" -> Method.PUBLISH;
      default -> throw new DecoderException(String.format("method (%s) not supported", method));
    };
  }

  @Override
  @SuppressWarnings("unchecked")
  public Flux<ByteBuf> encode(Publisher<Message<?>> in) {
    return Flux.from(in)
      .map(msg -> {
        var builder = new StringBuilder("time:" + encodeTimestamp(msg.getHeaders().getTimestamp()));

        if (msg.getHeaders().containsKey("topic"))
          builder.append(String.format("|name:%s", encodeTopic(msg.getHeaders().get("topic", String.class))));

        if (msg.getHeaders().containsKey("description"))
          builder.append(String.format("|descr:%s", msg.getHeaders().get("description", String.class)));

        ((Map<String, Object>) msg.getPayload())
          .forEach((k, v) -> builder.append(String.format("|%s:%s", k, v.toString())));

        return Unpooled.copiedBuffer(builder.append('\n').toString().getBytes(StandardCharsets.UTF_8));
      });
  }

  private String encodeTopic(String topic) {
    return topic.replace("vcas/", "");
  }

  private String encodeTimestamp(Long timestamp) {
    return new SimpleDateFormat(TIME_FMT).format(timestamp);
  }

  public enum Method {
    POLL, SUBSCRIBE, UNSUBSCRIBE, PUBLISH
  }
}
