package org.blab.sherpa.service;

import org.checkerframework.checker.units.qual.C;
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;

public class SessionTest {
  private static final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);

  private static IMqttAsyncClient createDefaultClient() {
    try {
      var client = mock(IMqttAsyncClient.class);

      doReturn(false).when(client).isConnected();

      doAnswer(
              a -> {
                doReturn(false).when(client).isConnected();
                return null;
              })
          .when(client)
          .close();

      doAnswer(
              a -> {
                var listener = (MqttActionListener) a.getArgument(2);

                scheduler.schedule(
                    () -> {
                      doReturn(true).when(client).isConnected();
                      listener.onSuccess(null);
                    },
                    3,
                    TimeUnit.SECONDS);

                return null;
              })
          .when(client)
          .connect(any(), any(), any(MqttActionListener.class));

      doAnswer(
              a -> {
                var listener = (MqttActionListener) a.getArgument(3);
                scheduler.schedule(() -> listener.onSuccess(null), 3, TimeUnit.SECONDS);
                return null;
              })
          .when(client)
          .subscribe(any(), any(), any(), any(MqttActionListener.class));

      doAnswer(
              a -> {
                var listener = (MqttActionListener) a.getArgument(2);
                scheduler.schedule(() -> listener.onSuccess(null), 3, TimeUnit.SECONDS);
                return null;
              })
          .when(client)
          .unsubscribe(any(), any(), any(MqttActionListener.class));

      doAnswer(
              a -> {
                var listener = (MqttActionListener) a.getArgument(5);
                scheduler.schedule(() -> listener.onSuccess(null), 3, TimeUnit.SECONDS);
                return null;
              })
          .when(client)
          .publish(any(), any(), any(), any(), any(), any(MqttActionListener.class));

      doAnswer(
              a -> {
                var callback = (MqttCallback) a.getArgument(0);

                scheduler.scheduleAtFixedRate(
                    () -> {
                      try {
                        callback.messageArrived("test", new MqttMessage(null));
                      } catch (Exception ignored) {
                      }
                    },
                    0,
                    3,
                    TimeUnit.SECONDS);

                return null;
              })
          .when(client)
          .setCallback(any(MqttCallback.class));

      return client;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void connect_ConnectSuccessFromFirstAttempt_Complete() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).expectComplete().verify();
  }

  @Test
  public void connect_ConnectSuccessFromThirdAttempt_Complete() throws MqttException {
    var client = createDefaultClient();
    var attempt = new AtomicInteger(1);

    doAnswer(
            a -> {
              var listener = (MqttActionListener) a.getArgument(2);

              if (attempt.getAndIncrement() < 3)
                scheduler.schedule(
                    () ->
                        listener.onFailure(
                            null, new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION)),
                    3,
                    TimeUnit.SECONDS);
              else scheduler.schedule(() -> listener.onSuccess(null), 3, TimeUnit.SECONDS);

              return null;
            })
        .when(client)
        .connect(any(), any(), any(MqttActionListener.class));

    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).expectNext(1L, 2L).expectComplete().verify();
  }

  @Test
  public void connect_ConnectCancelled_CompleteAndSessionClose() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).thenCancel().verify();
    StepVerifier.create(session.connect().result()).verifyError(SessionClosedException.class);
  }

  @Test
  public void connect_ConnectImmediateFatal_ErrorAndSessionClose() throws MqttException {
    var client = createDefaultClient();

    doThrow(MqttException.class).when(client).connect(any(), any(), any());

    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).verifyError(SessionException.class);
    StepVerifier.create(session.connect().result()).verifyError(SessionClosedException.class);
  }

  @Test
  public void connect_SessionClosed_Error() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.close().result()).verifyComplete();
    StepVerifier.create(session.connect().result()).verifyError(SessionClosedException.class);
  }

  @Test
  public void connect_ThenSessionClosed_Error() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);
    var connect =
        StepVerifier.create(session.listen().result())
            .expectError(SessionClosedException.class)
            .verifyLater();

    StepVerifier.create(session.close().result()).verifyComplete();
    connect.verify();
  }

  @Test
  public void connect_AlreadyCalled_Throws() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    session.connect();

    Assertions.assertThrows(SessionException.class, session::connect);
  }

  @Test
  public void listen_EventReceived_Next() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.listen().result()).expectNextCount(5).thenCancel().verify();
  }

  @Test
  public void listen_GlobalFatal_ErrorAndSessionClose() {
    var client = createDefaultClient();

    doAnswer(
            a -> {
              var callback = (MqttCallback) a.getArgument(0);

              scheduler.scheduleAtFixedRate(
                  () -> {
                    try {
                      callback.messageArrived("test", new MqttMessage(null));
                    } catch (Exception ignored) {
                    }
                  },
                  0,
                  10,
                  TimeUnit.SECONDS);

              scheduler.schedule(
                  () ->
                      callback.mqttErrorOccurred(
                          new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION)),
                  5,
                  TimeUnit.SECONDS);

              return null;
            })
        .when(client)
        .setCallback(any(MqttCallback.class));

    var session = new SynchronousSession(client);

    StepVerifier.create(session.listen().result())
        .expectNext(new Event("test", null))
        .verifyError(SessionException.class);
    StepVerifier.create(session.listen().result()).verifyError(SessionClosedException.class);
  }

  @Test
  public void listen_AlreadyCalled_Duplicate() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    Assertions.assertEquals(session.listen(), session.listen());
  }

  @Test
  public void listen_SessionClosed_Error() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.close().result()).verifyComplete();
    StepVerifier.create(session.listen().result()).verifyError(SessionClosedException.class);
  }

  @Test
  public void listen_SessionConnectingThenSessionClosed_Complete() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);
    var listen = StepVerifier.create(session.listen().result()).expectComplete().verifyLater();

    StepVerifier.create(session.close().result()).verifyComplete();
    listen.verify();
  }

  @Test
  public void poll_SessionConnectedAndValuePresented_CompleteWithEvent() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).verifyComplete();
    StepVerifier.create(session.poll("test").result())
        .expectNext(new Event("test", null))
        .verifyComplete();
  }

  @Test
  public void poll_SessionConnectedAndValueNotPresented_WaitUnitTimeout() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).verifyComplete();
    StepVerifier.create(session.poll("none").result()).verifyTimeout(Duration.ofSeconds(5));
  }

  @Test
  public void poll_SessionConnectedAndImmediateFatal_Error() throws MqttException {
    var client = createDefaultClient();

    doThrow(MqttException.class).when(client).subscribe(any(), 1, any(), any());

    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).verifyComplete();
    StepVerifier.create(session.poll("test").result()).verifyError(SessionException.class);
  }

  @Test
  public void poll_SessionConnectingThenConnectedThenValuePresented_CompleteWithEvent() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);
    var poll =
        StepVerifier.create(session.poll("test").result())
            .expectNext(new Event("test", null))
            .expectComplete()
            .verifyLater();

    StepVerifier.create(session.connect().result()).verifyComplete();
    poll.verify();
  }

  @Test
  public void poll_SessionConnectingThenClosed_CompleteEmpty() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);
    var poll = StepVerifier.create(session.poll("test").result()).expectComplete().verifyLater();

    StepVerifier.create(session.close().result()).verifyComplete();
    poll.verify();
  }

  @Test
  public void poll_SessionConnecting_WaitUntilTimeout() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.poll("test").result()).verifyTimeout(Duration.ofSeconds(5));
  }

  @Test
  public void poll_SessionClosed_Error() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.close().result()).verifyComplete();
    StepVerifier.create(session.poll("test").result()).verifyError(SessionClosedException.class);
  }

  @Test
  public void subscribe_SessionConnectedAndSuccess_CompleteWithTopic() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).verifyComplete();
    StepVerifier.create(session.subscribe("test").result()).expectNext("test").verifyComplete();
  }

  @Test
  public void subscribe_SessionConnectedAndImmediateFatal_Error() throws MqttException {
    var client = createDefaultClient();

    doThrow(MqttException.class).when(client).subscribe(any(), 1, any(), any());

    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).verifyComplete();
    StepVerifier.create(session.subscribe("test").result()).verifyError(SessionException.class);
  }

  @Test
  public void subscribe_SessionConnectedAndDelayedFatal_Error() throws MqttException {
    var client = createDefaultClient();

    doAnswer(
            a -> {
              var listener = (MqttActionListener) a.getArgument(3);
              scheduler.schedule(
                  () ->
                      listener.onFailure(
                          null, new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION)),
                  3,
                  TimeUnit.SECONDS);
              return null;
            })
        .when(client)
        .subscribe(any(), 1, any(), any(MqttActionListener.class));

    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).verifyComplete();
    StepVerifier.create(session.subscribe("test").result()).verifyError(SessionException.class);
  }

  @Test
  public void subscribe_SessionConnectingThenConnectedThenSuccess_CompleteWithTopic() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);
    var subscribe =
        StepVerifier.create(session.subscribe("test").result())
            .expectNext("test")
            .expectComplete()
            .verifyLater();

    StepVerifier.create(session.connect().result()).verifyComplete();
    subscribe.verify();
  }

  @Test
  public void subscribe_SessionConnecting_WaitUntilTimeout() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.subscribe("test").result()).verifyTimeout(Duration.ofSeconds(5));
  }

  @Test
  public void subscribe_SessionConnectingThenClosed_CompleteEmpty() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);
    var subscribe =
        StepVerifier.create(session.subscribe("test").result()).expectComplete().verifyLater();

    StepVerifier.create(session.close().result()).verifyComplete();
    subscribe.verify();
  }

  @Test
  public void subscribe_SessionClosed_Error() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.close().result()).verifyComplete();
    StepVerifier.create(session.subscribe("test").result())
        .verifyError(SessionClosedException.class);
  }

  @Test
  public void unsubscribe_SessionConnectedAndSuccess_CompleteWithTopic() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).verifyComplete();
    StepVerifier.create(session.unsubscribe("test").result()).expectNext("test").verifyComplete();
  }

  @Test
  public void unsubscribe_SessionConnectedAndImmediateFatal_Error() throws MqttException {
    var client = createDefaultClient();

    doThrow(MqttException.class).when(client).unsubscribe(any(), 1, any(), any());

    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).verifyComplete();
    StepVerifier.create(session.unsubscribe("test").result()).verifyError(SessionException.class);
  }

  @Test
  public void unsubscribe_SessionConnectedAndDelayedFatal_Error() throws MqttException {
    var client = createDefaultClient();

    doAnswer(
            a -> {
              var listener = (MqttActionListener) a.getArgument(2);

              scheduler.schedule(
                  () ->
                      listener.onFailure(
                          null, new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION)),
                  3,
                  TimeUnit.SECONDS);

              return null;
            })
        .when(client)
        .unsubscribe(any(), any(), any(MqttActionListener.class));

    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).verifyComplete();
    StepVerifier.create(session.subscribe("test").result()).verifyError(SessionException.class);
  }

  @Test
  public void unsubscribe_SessionConnectingThenConnectedThenSuccess_CompleteWithTopic() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);
    var unsubscribe =
        StepVerifier.create(session.unsubscribe("test").result())
            .expectNext("test")
            .expectComplete()
            .verifyLater();

    StepVerifier.create(session.connect().result()).verifyComplete();
    unsubscribe.verify();
  }

  @Test
  public void unsubscribe_SessionConnectingThenClosed_CompleteEmpty() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);
    var unsubscribe =
        StepVerifier.create(session.unsubscribe("test").result()).expectComplete().verifyLater();

    StepVerifier.create(session.close().result()).verifyComplete();
    unsubscribe.verify();
  }

  @Test
  public void unsubscribe_SessionConnecting_WaitUntilTimeout() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.unsubscribe("test").result()).verifyTimeout(Duration.ofSeconds(5));
  }

  @Test
  public void publish_SessionConnectedAndSuccess_CompleteWithEvent() {
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).verifyComplete();
    StepVerifier.create(session.publish(new Event("test", null)).result())
        .expectNext(new Event("test", null))
        .verifyComplete();
  }

  @Test
  public void publish_SessionConnectedAndImmediateFatal_Error() throws MqttException {
    var client = createDefaultClient();

    doThrow(SessionException.class)
        .when(client)
        .publish(any(), any(), any(), any(), any(), any(MqttActionListener.class));

    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).verifyComplete();
    StepVerifier.create(session.publish(new Event("test", null)).result())
        .verifyError(SessionException.class);
  }

  @Test
  public void publish_SessionConnectedAndDelayedFatal_Error() throws MqttException {
    var client = createDefaultClient();

    doAnswer(
            a -> {
              var listener = (MqttActionListener) a.getArgument(5);
              executor.schedule(
                  () -> listener.onFailure(null, new SessionException()), 3, TimeUnit.SECONDS);
              return null;
            })
        .when(client)
        .publish(any(), any(), any(), any(), any(), any(MqttActionListener.class));

    var session = new SynchronousSession(client);

    StepVerifier.create(session.connect().result()).verifyComplete();
    StepVerifier.create(session.publish(new Event("test", null)).result())
        .verifyError(SessionException.class);
  }

  @Test
  public void publish_SessionConnectingThenConnectedThenSuccess_CompleteWithEvent() {
    var event = new Event("test", null);
    var client = createDefaultClient();
    var session = new SynchronousSession(client);
    var publish =
        StepVerifier.create(session.publish(event).result())
            .expectNext(event)
            .expectComplete()
            .verifyLater();

    StepVerifier.create(session.connect().result()).verifyComplete();
    publish.verify();
  }

  @Test
  public void publish_SessionConnectingThenClosed_CompleteEmpty() {
    var event = new Event("test", null);
    var client = createDefaultClient();
    var session = new SynchronousSession(client);
    var publish =
        StepVerifier.create(session.publish(event).result()).expectComplete().verifyLater();

    StepVerifier.create(session.close().result()).verifyComplete();
    publish.verify();
  }

  @Test
  public void publish_SessionConnecting_WaitUntilTimeout() {
    var event = new Event("test", null);
    var client = createDefaultClient();
    var session = new SynchronousSession(client);

    StepVerifier.create(session.publish(event).result()).verifyTimeout(Duration.ofSeconds(5));
  }
}
