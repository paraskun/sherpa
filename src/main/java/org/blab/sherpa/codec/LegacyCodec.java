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
  public Publisher<Message<?>> decode(ByteBuf in) {
    return Mono.from();
  }

  private Message<?> decode(ByteBuf in) {
    return parse(in.toString(StandardCharsets.UTF_8));
  }

  private Message<?> parse(String msg) {
    var headers = new HashMap<String, Object>();
    var payload = new HashMap<String, Object>();

    Arrays.stream(msg.split("\\|"))
        .map(s -> s.split(":"))
        .filter(f -> f.length == 2)
        .filter(f -> !f[0].isBlank() && !f[1].isBlank())
        .map(f -> {
          f[0] = f[0].trim().toLowerCase();
          f[1] = f[1].trim();

          return f;
        })
        .forEach(f -> {
          switch (f[0]) {
            case "method", "meth", "m" ->
              headers.putIfAbsent(HEADERS_METHOD, decodeMethod(f[1]));
            case "name", "n" ->
              headers.putIfAbsent(HEADERS_TOPIC, f[1]);
            case "descr" ->
              headers.putIfAbsent(HEADERS_DESCRIPTION, f[1]);
            case "time" ->
              headers.putIfAbsent(HEADERS_TIMESTAMP, decodeTimestamp(f[1]));
            default -> payload.putIfAbsent(f[0], f[1]);
          }
        });

    var hdrs = new MessageHeaders(headers);

    if (!.containsKey(HEADERS_TOPIC))
      return new ErrorMessage(new HeaderNotFoundException("topic"), hdrs);

    if (!headers.containsKey(HEADERS_METHOD))
      return new ErrorMessage(new HeaderNotFoundException("method"), hdrs);
    else if (headers.get(HEADERS_METHOD).equals(null))
      return new ErrorMessage(new Unknown)

    return MessageBuilder.createMessage(payload, hdrs);
  }

  private Method decodeMethod(String method) {
    return switch (method) {
      case "get", "g", "getfull", "gf" -> Method.POLL;
      case "subscribe", "subscr", "sb" -> Method.SUBSCRIBE;
      case "release", "rel", "free", "f" -> Method.UNSUBSCRIBE;
      case "set", "s" -> Method.PUBLISH;
      default -> throw new UnknownMethodException(method);
    };
  }

  private Long decodeTimestamp(String time) {
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
    var response = switch (msg) {
      case ErrorMessage err -> switch (err.getPayload()) {};
      default -> {
        var builder = new StringBuilder(String.format("time:%d|name:%s|", 
              msg.getHeaders().get(HEADERS_TIMESTAMP, Long.class),
              msg.getHeaders().get(HEADERS_TOPIC, String.class)
              ));


      }
    }
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
