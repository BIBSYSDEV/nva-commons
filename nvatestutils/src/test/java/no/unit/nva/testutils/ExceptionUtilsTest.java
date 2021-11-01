package no.unit.nva.testutils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class ExceptionUtilsTest {

    public static final String TOP_MESSAGE = "Top message";
    public static final String NESTED_MESSAGE = "Nested message";

    @Test
    void stackTraceReturnsStackTraceStringOfException() {
        Exception actual = assertThrows(Exception.class, this::throwsException);
        String actualStackTrace = ExceptionUtils.stackTraceToString(actual);
        assertThat(actualStackTrace, containsString(TOP_MESSAGE));
        assertThat(actualStackTrace, containsString(NESTED_MESSAGE));
    }

    private void throwsException() throws Exception {
        Exception nestedException = new Exception(NESTED_MESSAGE);
        throw new Exception(TOP_MESSAGE, nestedException);
    }
}