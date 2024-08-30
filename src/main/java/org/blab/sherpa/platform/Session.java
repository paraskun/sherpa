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
   * <p>
   * Note that the timeout must be explicitly specified
   * by the consumer, otherwise operation may run indefinitely.
   * 
   * @return true, if operataion succeed;
   * @return empty, if the Session was closed by the client.
   * @return {@link PlatformException}, if platform-specific exception occurred.
   */
  Connect<Boolean> connect();

  /**
   * Poll the last {@link Message} from given topic.
   *
   * <p>
   * If this topic is already being watched, the last {@link Message}
   * will be used twice: here and in the Listen command.
   *
   * <p>
   * {@link Message} is logged and skipped when a deserialization exception occur.
   * The first normal {@link Message} will be returned.
   * 
   * <p>
   * Note that the timeout should be explicitly specified
   * by the consumer, otherwise operation may run indefinitely.
   *
   * @param topic channel to poll.
   *
   * @return latest {@link Message}.
   * @return empty, if the Session was closed by the client.
   * @return {@link PlatformException}, if platform-specific exception occurred.
   */
  Poll<Message<?>> poll(String topic);

  /**
   * Subscribe on the given topic.
   *
   * @param topic topic to observe.
   *
   * @return {@link Message} flow for given topic.
   * @return empty, if the Session was closed by the client.
   * @return {@link PlatformException}, if platform-specific exception occurred.
   */
  Subscribe<Message<?>> subscribe(String topic);

  /**
   * Unsubscribe from the given topic.
   *
   * @param topic topic to ignore.
   *
   * @return unsubscribed topic.
   * @return empty, if the Session was closed by the client.
   * @return {@link PlatformException}, if platform-specific exception occurred.
   */
  Unsubscribe<Boolean> unsubscribe(String topic);

  /**
   * Publish given {@link Message} on the platform.
   *
   * @param msg {@link Message} to be published.
   *
   * @return published {@link Message}.
   * @return empty, if the Session was closed by the client.
   * @return {@link PlatformException}, if platform-specific exception occurred.
   * @return {@link org.blab.sherpa.codec.CodecException}, if serialization
   *         exception occurred.
   */
  Publish<Boolean> publish(Message<?> msg);

  /**
   * Terminate all active operations and disconnect from the platform.
   */
  Close<Void> close();
}
