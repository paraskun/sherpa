package org.blab.sherpa.codec;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Component
@SuppressWarnings("unchecked")
public class LegacyCodec implements Codec<String> {
  public static final String TIME_FMT = "dd.MM.yyyy HH_mm_ss.SSS";

  public static final String HEADERS_METHOD = "_method";
  public static final String HEADERS_DESCRIPTION = "_description";

  private static final String ERROR_METHOD = "method (%s) not supported";
  private static final String ERROR_FIELD = "%s field not found";
  private static final String ERROR_UNKNOWN = "unknown";

  @Getter
  public enum Method {
    POLL, SUBSCRIBE, UNSUBSCRIBE, PUBLISH, UNKNOWN;

    private String value;

    public Method setValue(String value) {
      this.value = value;
      return this;
    }
  }

  @Override
  public Publisher<Message<?>> decode(String in) {
    var headers = new MessageHeaderAccessor();
    var payload = new HashMap<String, Object>();

    Arrays.stream(in.split("\\|"))
        .map(s -> s.split(":"))
        .filter(f -> f.length == 2)
        .filter(f -> !f[0].isBlank())
        .filter(f -> !f[1].isBlank())
        .map(f -> {
          f[0] = f[0].trim().toLowerCase();
          f[1] = f[1].trim();

          return f;
        })
        .forEach(f -> {
          switch (f[0]) {
            case "method", "meth", "m" ->
              headers.setHeaderIfAbsent(HEADERS_METHOD, decodeMethod(f[1]));
            case "name", "n" ->
              headers.setHeaderIfAbsent(HEADERS_TOPIC, f[1]);
            case "descr" ->
              headers.setHeaderIfAbsent(HEADERS_DESCRIPTION, f[1]);
            case "time" ->
              headers.setHeaderIfAbsent(HEADERS_TIMESTAMP, decodeTimestamp(f[1]));
            default -> payload.putIfAbsent(f[0], f[1]);
          }
        });

    headers.setHeaderIfAbsent(HEADERS_TIMESTAMP, System.currentTimeMillis());

    Message<?> message = MessageBuilder
        .withPayload(payload)
        .setHeaders(headers)
        .build();

    try {
      validate(message);
    } catch (CodecException e) {
      message = new ErrorMessage(e, message.getHeaders());
    }

    return Mono.just(message);
  }

  private Method decodeMethod(String method) {
    return switch (method) {
      case "get", "g", "getfull", "gf" -> Method.POLL;
      case "subscribe", "subscr", "sb" -> Method.SUBSCRIBE;
      case "release", "rel", "free", "f" -> Method.UNSUBSCRIBE;
      case "set", "s" -> Method.PUBLISH;
      default -> Method.UNKNOWN.setValue(method);
    };
  }

  private Long decodeTimestamp(String time) {
    try {
      return new SimpleDateFormat(TIME_FMT).parse(time).getTime();
    } catch (Exception e) {
      return System.currentTimeMillis();
    }
  }

  private void validate(Message<?> msg) {
    if (!msg.getHeaders().containsKey(HEADERS_TOPIC))
      throw new HeaderNotFoundException(HEADERS_TOPIC);

    if (!msg.getHeaders().containsKey(HEADERS_METHOD))
      throw new HeaderNotFoundException(HEADERS_METHOD);

    if (msg.getHeaders().get(HEADERS_METHOD).equals(Method.UNKNOWN))
      throw new UnknownMethodException(msg.getHeaders()
          .get(HEADERS_METHOD, Method.class)
          .getValue());
  }

  @Override
  public Publisher<String> encode(Message<?> in) {
    return Mono.just(
        switch (in) {
          case ErrorMessage e -> encodeError(e);
          default -> encodeEvent(in);
        });
  }

  private String encodeEvent(Message<?> in) {
    var response = new StringBuilder("time:" + in.getHeaders()
        .get(HEADERS_TIMESTAMP, Long.class));

    in.getHeaders().entrySet().forEach(e -> {
      switch (e.getKey()) {
        case HEADERS_TOPIC -> response.append("|name:" + e.getValue());
        case HEADERS_DESCRIPTION -> response.append("|descr:" + e.getValue());
      }
    });

    ((Map<String, Object>) in.getPayload()).forEach((k, v) -> {
      response.append(String.format("|%s:%s", k, v));
    });

    return response.append('\n').toString();
  }

  private String encodeError(ErrorMessage in) {
    var description = switch (in.getPayload()) {
      case HeaderNotFoundException e ->
        String.format(ERROR_FIELD, e.getHeaderName().equals(HEADERS_TOPIC) ? "name" : e.getHeaderName().substring(1));
      case UnknownMethodException e ->
        String.format(ERROR_METHOD, e.getMethodName());
      default -> ERROR_UNKNOWN;
    };

    return String.format("time:%d|name:%s|val:error|descr:%s\n",
        in.getHeaders()
            .get(HEADERS_TIMESTAMP, Long.class),
        in.getHeaders()
            .getOrDefault(HEADERS_TOPIC, "error"),
        description);
  }
}
