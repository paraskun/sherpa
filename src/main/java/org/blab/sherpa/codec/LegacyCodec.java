package org.blab.sherpa.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.log4j.Log4j2;

import org.reactivestreams.Publisher;
import org.springframework.core.codec.DecodingException;
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

@Log4j2
@Component
public class LegacyCodec implements Codec<Message<?>> {
  public static final String HEADERS_METHOD = "_method";
  public static final String HEADERS_DESCRIPTION = "_description";

  private static final String TIME_FMT = "dd.MM.yyyy HH_mm_ss.SSS";

  @Override
  public Flux<Message<?>> decode(Publisher<ByteBuf> in) {
    return Flux.from(in).map(buff -> {
      var payload = new HashMap<String, Object>();
      var headers = new HashMap<String, Object>();

      try {
        var fields = parse(buff.toString(StandardCharsets.UTF_8));

        fields.forEach((key, value) -> {
          switch (key) {
            case "method", "meth", "m" -> decodeMethod(value, headers);
            case "name", "n" -> decodeTopic(value, headers);
            case "descr" -> decodeDescription(value, headers);
            case "time" -> decodeTimestamp(value, headers);
            default -> payload.put(key, value);
          }
        });

        if (!headers.containsKey(HEADERS_TOPIC))
          throw new DecodingException("name field not found");

        if (!headers.containsKey(HEADERS_METHOD))
          throw new DecodingException("method field not found");

        return MessageBuilder.createMessage(payload, new MessageHeaders(headers));
      } catch (DecodingException e) {
        return new ErrorMessage(e, new MessageHeaders(headers));
      }
    });
  }

  private Map<String, String> parse(String msg) {
    return Arrays.stream(msg.trim().split("\\|"))
      .map(s -> s.split(":"))
      .filter(s -> s.length == 2)
      .map(s -> {
        s[0] = s[0].trim().toLowerCase();
        return s;
      })
      .collect(Collectors.toMap(s -> s[0], s -> s[1]));
  }

  private void decodeTopic(String topic, HashMap<String, Object> headers) {
    headers.put(HEADERS_TOPIC, topic);
  }

  private void decodeTimestamp(String time, HashMap<String, Object> headers) {
    try {
      headers.put(HEADERS_TIMESTAMP, new SimpleDateFormat(TIME_FMT).parse(time).getTime());
    } catch (Exception e) {
      log.debug(e);
    }
  }

  private void decodeMethod(String method, HashMap<String, Object> headers) {
    switch (method) {
      case "get", "g", "getfull", "gf" -> headers.put(HEADERS_METHOD, Method.POLL);
      case "subscribe", "subscr", "sb" -> headers.put(HEADERS_METHOD, Method.SUBSCRIBE);
      case "release", "rel", "free", "f" -> headers.put(HEADERS_METHOD, Method.UNSUBSCRIBE);
      case "set", "s" -> headers.put(HEADERS_METHOD, Method.PUBLISH);
      default -> throw new DecodingException(String.format("method (%s) not supported", method));
    };
  }

  private void decodeDescription(String description, HashMap<String, Object> headers) {
    headers.put(HEADERS_DESCRIPTION, description);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Flux<ByteBuf> encode(Publisher<Message<?>> in) {
    return Flux.from(in)
      .map(msg -> {
        var builder = new StringBuilder("time:" + encodeTimestamp(getTimestamp(msg.getHeaders())));

        if (msg.getHeaders().containsKey(HEADERS_DESCRIPTION))
          builder.append(String.format("|descr:%s", msg.getHeaders().get("description", String.class)));

        ((Map<String, Object>) msg.getPayload())
          .forEach((k, v) -> builder.append(String.format("|%s:%s", k, v.toString())));

        return Unpooled.copiedBuffer(builder.append('\n').toString().getBytes(StandardCharsets.UTF_8));
      });
  }

  private Long getTimestamp(MessageHeaders headers) {
    return headers.containsKey(HEADERS_TIMESTAMP) ?
      headers.get(HEADERS_TIMESTAMP, Long.class) :
      headers.getTimestamp();
  }

  private String encodeTimestamp(Long timestamp) {
    return new SimpleDateFormat(TIME_FMT).format(timestamp);
  }

  public enum Method {
    POLL, SUBSCRIBE, UNSUBSCRIBE, PUBLISH
  }
}
