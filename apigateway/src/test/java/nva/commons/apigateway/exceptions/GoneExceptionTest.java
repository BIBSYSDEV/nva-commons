package nva.commons.apigateway.exceptions;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

class GoneExceptionTest {

  public static final String SOME_MESSAGE = "Some message";
  private static final String CAUSE_MESSAGE = "Cause message";

  @Test
  void goneExceptionShowsSuppliedMessage() {
    GoneException exception = new GoneException(SOME_MESSAGE);
    assertThat(exception.getMessage(), containsString(SOME_MESSAGE));
  }

  @Test
  void notFoundExceptionShowsSuppliedCause() {
    Exception cause = new Exception(CAUSE_MESSAGE);
    GoneException exception = new GoneException(cause);
    ByteArrayOutputStream stackTraceOutput = getStackTraceMessage(exception);
    assertThat(exception.getCause(), is(equalTo(cause)));
    assertThat(stackTraceOutput.toString(UTF_8), containsString(CAUSE_MESSAGE));
  }

  @Test
  void notFoundExceptionShowsSuppliedMessageAndSuppliedCause() {
    Exception cause = new Exception(CAUSE_MESSAGE);
    GoneException exception = new GoneException(cause, SOME_MESSAGE);
    ByteArrayOutputStream stackTraceOutput = getStackTraceMessage(exception);
    assertThat(exception.getCause(), is(equalTo(cause)));
    String stackTraceOutputString = stackTraceOutput.toString(UTF_8);
    assertThat(stackTraceOutputString, containsString(CAUSE_MESSAGE));
    assertThat(stackTraceOutputString, containsString(SOME_MESSAGE));
  }

  @Test
  void notFoundExceptionReturnsNotFoundStatusCode() {
    GoneException exception = new GoneException(SOME_MESSAGE);
    assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_GONE)));
  }

  private ByteArrayOutputStream getStackTraceMessage(GoneException exception) {
    ByteArrayOutputStream stackTraceOutput = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(stackTraceOutput, true, UTF_8);
    exception.printStackTrace(writer);
    writer.close();
    return stackTraceOutput;
  }
}
