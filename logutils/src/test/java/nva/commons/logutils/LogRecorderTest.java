package nva.commons.logutils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class LogRecorderTest {

  @Test
  void shouldCaptureEventsLoggedByTheTargetClassWhenUsingForClass() {
    var recorder = LogRecorder.forClass(AnotherDummyClassForLogTesting.class);

    new AnotherDummyClassForLogTesting().logInfo("hello world");

    assertThat(recorder.messages()).anyMatch(message -> message.contains("hello world"));
  }

  @Test
  void shouldCaptureEventsFromArbitraryLoggersWhenUsingForRoot() {
    var recorder = LogRecorder.forRoot(LogRecorderTest.class);

    LoggerFactory.getLogger("nva.commons.logutils.adhoc").info("root capture {}", "ok");

    assertThat(recorder.messages()).anyMatch(message -> message.contains("root capture ok"));
  }

  @Test
  void shouldHaveNoMessagesAfterClearing() {
    var recorder = LogRecorder.forRoot(LogRecorderTest.class);
    new AnotherDummyClassForLogTesting().logInfo("hello world");
    recorder.clear();

    assertThat(recorder.messages()).isEmpty();
  }

  @Test
  void shouldIncludeThrownMessageInMessagesWhenEventHasAnException() {
    var recorder = LogRecorder.forClass(AnotherDummyClassForLogTesting.class);

    new AnotherDummyClassForLogTesting().logError("boom", new IllegalStateException("inner cause"));

    assertThat(recorder.events()).hasSize(1);
    assertThat(recorder.messages()).anyMatch(message -> message.contains("boom inner cause"));
  }

  @Test
  void shouldJoinAllCapturedMessagesWhenCallingAsString() {
    var recorder = LogRecorder.forClass(AnotherDummyClassForLogTesting.class);

    var subject = new AnotherDummyClassForLogTesting();
    subject.logInfo("first");
    subject.logInfo("second");

    assertThat(recorder.asString()).isEqualTo("first second");
  }

  @Test
  void shouldReturnEmptyRecorderFromForClassEvenWhenPriorEventsWereCaptured() {
    var first = LogRecorder.forClass(AnotherDummyClassForLogTesting.class);
    new AnotherDummyClassForLogTesting().logInfo("noise from a prior test");
    assertThat(first.messages()).isNotEmpty();

    var second = LogRecorder.forClass(AnotherDummyClassForLogTesting.class);

    assertThat(second.messages()).isEmpty();
  }
}
