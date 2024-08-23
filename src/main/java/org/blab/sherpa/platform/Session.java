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
   * @return {@link PlatformException}, if execution exception occurred.
   */
  Connect<Boolean> connect();

  /**
   * Listen for the new {@link Message}s in subscribed topic.
   *
   * @return stream of {@link Message}s from subscribed topics.
   * @return {@link PlatformException}, if execution exception occurred.
   * @return {@link org.blab.sherpa.codec.Codec}, if deserialization exception
   *         occurred.
   */
  Listen<Message<?>> listen();

  /**
   * Poll the last {@link Message} from given topic.
   *
   * <p>
   * If this topic is already being watched, the last {@link Message}
   * will be used twice: here and in the Listen command.
   *
   * <p>
   * Note that the timeout should be explicitly specified
   * by the consumer, otherwise operation may run indefinitely.
   *
   * @param topic latest {@link Message} in given topic.
   *
   * @return latest {@link Message}.
   * @return {@link PlatformException}, if execution exception occurred.
   * @return {@link org.blab.sherpa.codec.CodecException}, if deserialization
   *         exception occurred.
   */
  Poll<Message<?>> poll(String topic);

  /**
   * Subscribe on the given topic.
   *
   * @param topic topic to observe.
   *
   * @return subscribed topic.
   * @return {@link PlatformException}, if execution exception occurred.
   */
  Subscribe<String> subscribe(String topic);

  /**
   * Unsubscribe from the given topic.
   *
   * @param topic topic to ignore.
   *
   * @return unsubscribed topic.
   * @return {@link PlatformException}, if execution exception occurred.
   */
  Unsubscribe<String> unsubscribe(String topic);

  /**
   * Publish given {@link Message} on the platform.
   *
   * @param msg {@link Message} to be published.
   *
   * @return published {@link Message}.
   * @return {@link PlatformException}, if execution exception occurred.
   * @return {@link org.blab.sherpa.codec.CodecException}, if serialization
   *         exception occurred.
   */
  Publish<Message<?>> publish(Message<?> msg);

  /**
   * Terminate all active operations and disconnect from the platform.
   */
  Close<Void> close();
}
