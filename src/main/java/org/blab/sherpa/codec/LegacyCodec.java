package org.blab.sherpa.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.reactivestreams.Publisher;
import org.springframework.core.codec.CodecException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
@SuppressWarnings("unchecked")
public class LegacyCodec implements Codec<ByteBuf> {
  public static final String TIME_FMT = "dd.MM.yyyy HH_mm_ss.SSS";
  public static final String HEADERS_METHOD = "_method";

  public enum Method {
    POLL, SUBSCRIBE, UNSUBSCRIBE, PUBLISH
  }

  @Override
  public Flux<Message<?>> decode(Publisher<ByteBuf> in) {
    return Flux.from(in).map(this::decode);
  }

  private Message<?> decode(ByteBuf in) {
    try {
      return parse(in.toString(StandardCharsets.UTF_8));
    } catch (CodecException e) {
      return new ErrorMessage(e);
    }
  }

  private Message<?> parse(String msg) {
    var headers = new HashMap<String, Object>();
    var payload = new HashMap<String, Object>();

    Arrays.stream(msg.trim().split("\\|"))
      .map(s -> s.split(":"))
      .filter(s -> s.length == 2 && !s[1].isBlank())
      .forEach(e -> {
           switch (e[0]) {
            case "method", "meth", "m" -> headers.putIfAbsent(HEADERS_METHOD, parseMethod(e[1]));
            case "name", "n" -> headers.putIfAbsent(HEADERS_TOPIC, parseTopic(e[1]));
            case "descr" -> headers.putIfAbsent(HEADERS_DESCRIPTION, parseDescription(e[1]));
            case "time" -> headers.putIfAbsent(HEADERS_TIMESTAMP, parseTimestamp(e[1]));
            default -> payload.put(e[0], e[1]);
          }       
      });
  
    var hdrs = new MessageHeaders(headers);

    if (!headers.containsKey(HEADERS_TOPIC))
      return new ErrorMessage(new CodecException("name field not found"), hdrs);

    if (!headers.containsKey(HEADERS_METHOD))
      return new ErrorMessage(new CodecException("method field not found"), hdrs);

    return MessageBuilder.createMessage(payload, hdrs);
  }

  private Method parseMethod(String method) {
    return switch (method) {
      case "get", "g", "getfull", "gf" -> Method.POLL;
      case "subscribe", "subscr", "sb" -> Method.SUBSCRIBE;
      case "release", "rel", "free", "f" -> Method.UNSUBSCRIBE;
      case "set", "s" -> Method.PUBLISH;
      default -> throw new CodecException(String.format("method (%s) not supported", method));
    };
  }

  private String parseTopic(String topic) {
    return topic;
  }

  private String parseDescription(String description) {
    return description;
  }

  private Long parseTimestamp(String time) {
    try {
      return new SimpleDateFormat(TIME_FMT).parse(time).getTime();
    } catch (Exception e) {
      return System.currentTimeMillis();
    }
  }

  @Override
  public Flux<ByteBuf> encode(Publisher<Message<?>> in) {
    return Flux.from(in).flatMap(this::encode);
  }

  private Mono<ByteBuf> encode(Message<?> msg) {
    var resp = new StringBuilder("time:" + getTimestamp(msg));

    resp.append("|name:" + msg.getHeaders().getOrDefault(HEADERS_TOPIC, "error"));

    if (msg instanceof ErrorMessage err)
      resp.append(String.format("|val:error|descr:%s", err.getPayload().getMessage()));
    else {
      resp.append("|descr:" + msg.getHeaders().getOrDefault(HEADERS_DESCRIPTION, "null"));

      ((Map<String, Object>) msg.getPayload())
        .forEach((k, v) -> resp.append(String.format("|%s:%s", k, v.toString())));
    }

    return Mono.just(Unpooled.copiedBuffer(resp.append('\n')
          .toString()
          .getBytes(StandardCharsets.UTF_8)));

  }

  private String getTimestamp(Message<?> msg) {
    return encodeTimestamp((Long) msg.getHeaders()
      .getOrDefault(HEADERS_TIMESTAMP, msg.getHeaders().getTimestamp()));
  }

  private String encodeTimestamp(Long timestamp) {
    return new SimpleDateFormat(TIME_FMT).format(timestamp);
  }
}

