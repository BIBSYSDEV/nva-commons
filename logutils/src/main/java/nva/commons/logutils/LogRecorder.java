package nva.commons.logutils;

import static java.util.Objects.nonNull;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;

/**
 * Records log events emitted during a test. Unlike {@link LogUtils}, this helper does not
 * reconfigure the Log4j context: it attaches a {@link ListAppender} to either a single class logger
 * or the root logger, leaving the rest of the configuration untouched.
 */
public final class LogRecorder {

  private static final String ROOT_RECORDER_NAME = "RootLogRecorder";
  private static final String SPACE = " ";

  private final ListAppender delegate;

  private LogRecorder(ListAppender delegate) {
    this.delegate = delegate;
  }

  /**
   * Start a recorder on the logger for the given class. The returned recorder is empty: any
   * previously captured events on the same logger are discarded. Tests that share a recorder across
   * methods (for example, obtained once in {@code @BeforeAll}) should call {@link #clear()}
   * themselves between tests.
   *
   * @param clazz the class whose logger should be captured.
   */
  public static LogRecorder forClass(Class<?> clazz) {
    anchorLoggerContextViaSlf4j(clazz);
    var classLogger = (Logger) LogManager.getLogger(clazz);
    var loggerContext = classLogger.getContext();
    return freshRecorder(resolveAppender(loggerContext, classLogger, clazz.getSimpleName()));
  }

  /**
   * Start a recorder on the root logger. The returned recorder is empty: any previously captured
   * events on the root logger are discarded. The {@code caller} class is used to resolve the
   * correct {@link LoggerContext}; pass the test class itself.
   *
   * @param caller the test class invoking this helper, used to resolve the right LoggerContext.
   */
  public static LogRecorder forRoot(Class<?> caller) {
    anchorLoggerContextViaSlf4j(caller);
    var callerLogger = (Logger) LogManager.getLogger(caller);
    var loggerContext = callerLogger.getContext();
    return freshRecorder(
        resolveAppender(loggerContext, loggerContext.getRootLogger(), ROOT_RECORDER_NAME));
  }

  private static LogRecorder freshRecorder(ListAppender appender) {
    appender.clear();
    return new LogRecorder(appender);
  }

  /** Formatted messages of all captured events, in the order they were emitted. */
  public List<String> messages() {
    return delegate.getEvents().stream().map(LogRecorder::eventToString).toList();
  }

  /** All captured log events, in the order they were emitted. */
  public List<LogEvent> events() {
    return delegate.getEvents();
  }

  /** Space-joined rendering of all captured messages. Thrown exception messages are appended. */
  public String asString() {
    return String.join(SPACE, messages());
  }

  /** Discard all captured events. */
  public void clear() {
    delegate.clear();
  }

  /**
   * Resolves the {@link LoggerContext} via the SLF4J binding first so it matches the one production
   * code uses via {@link org.slf4j.LoggerFactory#getLogger(Class)}. Without this, the subsequent
   * {@link LogManager#getLogger(Class)} call can land in a different context and the recorder will
   * never see events.
   */
  private static void anchorLoggerContextViaSlf4j(Class<?> clazz) {
    org.slf4j.LoggerFactory.getLogger(clazz);
  }

  private static ListAppender resolveAppender(
      LoggerContext loggerContext, Logger logger, String appenderName) {
    var existing = loggerContext.getConfiguration().getAppenders().get(appenderName);
    if (existing instanceof ListAppender listAppender) {
      return listAppender;
    }
    var appender = new ListAppender(appenderName);
    appender.start();
    loggerContext.getConfiguration().addLoggerAppender(logger, appender);
    return appender;
  }

  private static String eventToString(LogEvent event) {
    var message = event.getMessage().getFormattedMessage();
    if (nonNull(event.getThrown())) {
      return message + SPACE + event.getThrown().getMessage();
    }
    return message;
  }
}
