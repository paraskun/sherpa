package org.blab.sherpa.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class HandlerChainTest {
  @Test
  public void handle_EmptyChain_Cancel() {
    Assertions.assertNull(HandlerChain.builder().build().handle(new Object()));
    Assertions.assertNull(HandlerChain.builder().build().handle(null));
  }

  @Test
  public void handle_CorruptedChain_Throws() {
    var checked = new AtomicBoolean(false);
    var chain =
        HandlerChain.builder()
            .add(
                (o) -> {
                  System.out.println("Running");
                  checked.set(true);
                  return null;
                })
            .add(
                (o) -> {
                  throw new RuntimeException();
                })
            .add(
                (o) -> {
                  checked.set(false);
                  return null;
                })
            .build();

    Assertions.assertThrows(RuntimeException.class, () -> chain.handle(null));
    Assertions.assertTrue(checked.get());
  }

  @Test
  public void handle_CompletedChain_Return() {
    var chain =
        HandlerChain.builder()
            .add((o) -> null)
            .add((o) -> new Object())
            .add(
                (o) -> {
                  throw new RuntimeException();
                })
            .build();

    var result = Assertions.assertDoesNotThrow(() -> chain.handle(null));
    Assertions.assertInstanceOf(Object.class, result);
  }

  @Test
  public void handle_CancelledChain_Cancel() {
    var chain = HandlerChain.builder().add((o) -> null).add((o) -> null).build();
    var result = Assertions.assertDoesNotThrow(() -> chain.handle(null));
    Assertions.assertNull(result);
  }
}
