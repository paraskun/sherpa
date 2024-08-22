package org.blab.sherpa.platform;

import org.springframework.messaging.Message;

/**
 * An abstraction of the target platform used
 * to operate against it.
 */
public interface Session {
  /**
   * Connect to the platform.
   *
   * Note that the timeout must be explicitly specified
   * by the consumer, otherwise operation may run indefinitely.
   * 
   * @return true, if operataion succeed;
   * @return emptry, if the session was closed or {@link PlatformException} occurs;
   * @return {@link java.util.concurrent.ExecutionException}
   */
  Connect<Boolean> connect();

  /**
   * Listen for the new {@link Message}s in subscribed topic.
   *
   * If a fatal platform exception occurs, terminates with an error.
   * If a session is close, terminates with nothing.
   *
   * @return stream of {@link Message}s from subscribed topics.
   */
  Listen<Message<?>> listen();

  /**
   * Poll the latest {@link Message} from given topic.
   *
   * Note that the timeout should be explicitly specified
   * by the consumer, otherwise operation may run indefinitely.
   *
   * If a fatal platform exception occurs, terminates with an error.
   * If a session is close, terminates with nothing.
   *
   * @param topic latest {@link Message} in given topic.
   * @return latest {@link Message}.
   */
  Poll<Message<?>> poll(String topic);

  /**
   * Subscribe on the given topic.
   *
   * @param topic topic to observe.
   * @return subscribed topic, if the operation succeeds.
   * @return emptry, if the session was closed or
   *         a {@link PlatformException} occurs.
   */
  Subscribe<String> subscribe(String topic);

  /**
   * Unsubscribe from the given topic.
   *
   * If a fatal platform exception occurs, terminates with an error.
   * If a session is close, terminates with nothing.
   *
   * @param topic topic to ignore.
   * @return unsubscribed topic.
   */
  Unsubscribe<String> unsubscribe(String topic);

  /**
   * Publish given {@link Message} on the platform.
   *
   * If a fatal platform exception occurs, terminates with an error.
   * If a session is close, terminates with nothing.
   *
   * @param msg {@link Message} to be published.
   * @return published {@link Message}.
   */
  Publish<Message<?>> publish(Message<?> msg);

  /**
   * Terminate all active operations and disconnect from the platform.
   *
   * If a fatal platform exception occurs, terminates with an error.
   */
  Close<Void> close();
}
