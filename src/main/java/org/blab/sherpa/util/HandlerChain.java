package org.blab.sherpa.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Function;

public class HandlerChain<T, V> extends LinkedList<Function<T, V>> {
  public HandlerChain() {
    super();
  }

  public HandlerChain(Collection<? extends Function<T, V>> base) {
    super(base);
  }

  public static <T, V> HandlerChainBuilder<T, V> builder() {
    return new HandlerChainBuilder<>();
  }

  public static <T, V> HandlerChainBuilder<T, V> builder(
      Collection<? extends Function<T, V>> base) {
    return new HandlerChainBuilder<>(base);
  }

  public V handle(T t) {
    V result = null;

    for (var iterator = iterator(); iterator.hasNext() && result == null; )
      result = iterator.next().apply(t);

    return result;
  }

  public static class HandlerChainBuilder<T, V> {
    private final HandlerChain<T, V> chain;

    public HandlerChainBuilder() {
      this.chain = new HandlerChain<>();
    }

    public HandlerChainBuilder(Collection<? extends Function<T, V>> base) {
      this.chain = new HandlerChain<>(base);
    }

    public HandlerChainBuilder<T, V> add(Function<T, V> f) {
      chain.add(f);
      return this;
    }

    public HandlerChain<T, V> build() {
      return chain;
    }
  }
}
