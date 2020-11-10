package nva.commons.utils;

import static nva.commons.utils.ExceptionUtils.stackTraceInSingleLine;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class ExceptionUtilsTest {

    public static final String NEW_LINE = System.lineSeparator();

    @Test
    public void stackTraceInSingleLineReturnsExceptionMessageInOneLine() {
        Executable action = () -> throwsException();
        ArithmeticException exception = assertThrows(ArithmeticException.class, action);
        verifyOriginalMessageContainsNewLine(exception);

        String exceptionMessage = stackTraceInSingleLine(exception);
        assertThat(exceptionMessage, is(not(nullValue())));
        assertThat(exceptionMessage, not(containsString(NEW_LINE)));
    }

    @Test
    public void stackTraceInSingleLineThrowsNoExceptionWhenThereIsNoMessage() {
        Exception exception = new Exception((String) null);
        Executable action = () -> stackTraceInSingleLine(exception);
        assertDoesNotThrow(action);
    }

    private void verifyOriginalMessageContainsNewLine(ArithmeticException exception) {
        assertThat(originalMessage(exception), is(not(nullValue())));
        assertThat(originalMessage(exception), containsString(NEW_LINE));
    }

    private int throwsException() {
        return 1 / 0;
    }

    private String originalMessage(Exception e) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(outputStream);
        e.printStackTrace(pw);
        pw.close();
        return outputStream.toString(StandardCharsets.UTF_8);
    }
}