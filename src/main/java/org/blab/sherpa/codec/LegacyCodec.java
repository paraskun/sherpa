package org.blab.sherpa.codec;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.Arrays;

/** {@link Codec} for legacy messaging protocol (known as VCAS). */
@Log4j2
@Component
public class LegacyCodec implements Codec<String> {
  public static final String TIME_FMT = "dd.MM.yyyy HH_mm_ss.SSS";

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
    var builder = LegacyEvent.builder();

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
              builder.method(decodeMethod(f[1]));
            case "name", "n" ->
              builder.topic(f[1]);
            case "descr" ->
              builder.header(f[0], f[1]);
            case "time" ->
              builder.timestamp(decodeTimestamp(f[1]));
            default ->
              builder.field(f[0], f[1]);
          }
        });

    builder.timestamp(System.currentTimeMillis());

    try {
      var event = builder.build();

      if (event.getMethod().equals(Method.UNKNOWN))
        throw new UnknownMethodException(event.getHeaders(), event.getMethod().getValue());

      return Mono.just(event);
    } catch (CodecException e) {
      return Mono.just(new ErrorMessage(e, e.getHeaders()));
    }
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

  @Override
  public Publisher<String> encode(Message<?> in) {
    return Mono.just(
        switch (in) {
          case ErrorMessage e -> encodeError(e);
          case Event e -> encodeEvent(e);
          default -> throw new RuntimeException("Unsupported message type.");
        });
  }

  private String encodeEvent(Event event) {
    var response = new StringBuilder();

    response.append("time:" + event.getTimestamp());
    response.append("|name:" + event.getTopic());

    event.getHeadersUnsafe().entrySet().stream()
        .filter(es -> !es.getKey().startsWith("_"))
        .forEach(es -> response.append("|" + es.getKey() + ":" + es.getValue()));

    event.forEach((k, v) -> {
      response.append(String.format("|%s:%s", k, v));
    });

    return response.append('\n').toString();
  }

  private String encodeError(ErrorMessage err) {
    var response = new StringBuilder()
        .append("time:" + err.getHeaders().get(Event.HDRS_TIMESTAMP))
        .append("|name:" + err.getHeaders().getOrDefault(Event.HDRS_TOPIC, "error"))
        .append("|val:error");

    var description = switch (err.getPayload()) {
      case HeaderMissedException e ->
        String.format(ERROR_FIELD, e.getHeaderName()
            .equals(Event.HDRS_TOPIC) ? "name" : e.getHeaderName().substring(1));
      case UnknownMethodException e ->
        String.format(ERROR_METHOD, e.getMethodName());
      default -> ERROR_UNKNOWN;
    };

    return response
        .append("|descr:" + description)
        .append('\n')
        .toString();
  }
}
