package nva.commons.apigateway.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class ConflictExceptionTest {

    public static final String SOME_MESSAGE = "Some message";
    private static final String CAUSE_MESSAGE = "Cause message";

    @Test
    public void conflictExceptionShowsSuppliedMessage() {
        ConflictException exception = new ConflictException(SOME_MESSAGE);
        assertThat(exception.getMessage(), containsString(SOME_MESSAGE));
    }

    @Test
    public void conflictExceptionShowsSuppliedCause() {
        Exception cause = new Exception(CAUSE_MESSAGE);
        ConflictException exception = new ConflictException(cause);
        ByteArrayOutputStream stackTraceOutput = getStackTraceMessage(exception);
        assertThat(exception.getCause(), is(equalTo(cause)));
        assertThat(stackTraceOutput.toString(), containsString(CAUSE_MESSAGE));
    }

    @Test
    public void conflictExceptionShowsSuppliedMessageAndSuppliecCause() {
        Exception cause = new Exception(CAUSE_MESSAGE);
        ConflictException exception = new ConflictException(cause, SOME_MESSAGE);
        ByteArrayOutputStream stackTraceOutput = getStackTraceMessage(exception);
        assertThat(exception.getCause(), is(equalTo(cause)));
        String stackTraceOutputString = stackTraceOutput.toString();
        assertThat(stackTraceOutputString, containsString(CAUSE_MESSAGE));
        assertThat(stackTraceOutputString, containsString(SOME_MESSAGE));
    }

    @Test
    public void conflictExceptionReturnsNotFoundStatusCode() {
        ConflictException exception = new ConflictException(SOME_MESSAGE);
        assertThat(exception.getStatusCode(), is(equalTo(HttpStatus.SC_CONFLICT)));
    }

    private ByteArrayOutputStream getStackTraceMessage(ConflictException exception) {
        ByteArrayOutputStream stackTraceOutput = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(stackTraceOutput, true);
        exception.printStackTrace(writer);
        writer.close();
        return stackTraceOutput;
    }
}