package nva.commons.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ApiGatewayExceptionTest {

    private static final String MESSAGE = "someMessage";

    @Test
    public void apiGatewayExceptionHasConstructorForMessage() {
        ApiGatewayException exception = new TestException(MESSAGE);
        assertThat(exception.getMessage(), is(equalTo(MESSAGE)));
    }

    @Test
    public void apiGatewayExceptionHasConstructorForException() {
        IOException inner = new IOException(MESSAGE);
        ApiGatewayException exception = new TestException(inner);
        assertThat(exception.getMessage(), containsString(MESSAGE));
    }

    @Test
    public void apiGatewayExceptionContainsInnerClassInMessage() {
        IOException inner = new IOException(MESSAGE);
        ApiGatewayException exception = new TestException(inner);
        assertThat(exception.getMessage(), containsString(MESSAGE));
        assertThat(exception.getMessage(), containsString(IOException.class.getName()));
    }

    private static class TestException extends ApiGatewayException {

        public TestException(String message) {
            super(message);
        }

        public TestException(Exception e) {
            super(e);
        }

        @Override
        public Integer statusCode() {
            return 0;
        }
    }
}
