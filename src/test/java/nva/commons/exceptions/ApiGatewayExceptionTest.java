package nva.commons.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.Method;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ApiGatewayExceptionTest {

    private static final String MESSAGE = "someMessage";
    public static final String STATUS_CODE_METHOD_NAME = "statusCode";
    public static final String INT_PRIMITIVE_TYPE = "int";

    @Test
    @DisplayName("apiGatewayException has a constructor for message")
    public void apiGatewayExceptionHasConstructorForMessage() {
        ApiGatewayException exception = new TestException(MESSAGE);
        assertThat(exception.getMessage(), is(equalTo(MESSAGE)));
    }

    @Test
    @DisplayName("apiGatewayException has a constructor for exception")
    public void apiGatewayExceptionHasConstructorForException() {
        IOException inner = new IOException(MESSAGE);
        ApiGatewayException exception = new TestException(inner);
        assertThat(exception.getMessage(), containsString(MESSAGE));
    }

    @Test
    @DisplayName("apiGatewayException contains innerClass name in the message when it is instantiated with an "
        + "exception")
    public void apiGatewayExceptionContainsInnerClassInMessage() {
        IOException inner = new IOException(MESSAGE);
        ApiGatewayException exception = new TestException(inner);
        assertThat(exception.getMessage(), containsString(MESSAGE));
        assertThat(exception.getMessage(), containsString(IOException.class.getName()));
    }

    @Test
    @DisplayName("getStatusCode throws IllegalStateException when statusCode has not been set")
    public void getStatusCodeThrowsIllegalStateExceptionWhenStatusCodeHasNotBeenSet() {
        ApiGatewayException exception = new ExceptionWithoutStatusCode(MESSAGE);

        IllegalStateException actual = assertThrows(IllegalStateException.class, exception::getStatusCode);
        assertThat(actual.getMessage(), containsString(exception.getClass().getCanonicalName()));
    }

    @Test
    @DisplayName("statusCode returns Integer and not int")
    public void statusCodeReturnsIntegerAndNotInt() throws NoSuchMethodException {
        Method statusCodeMethod = ApiGatewayException.class.getDeclaredMethod(STATUS_CODE_METHOD_NAME);
        String typeName = statusCodeMethod.getGenericReturnType().getTypeName();
        assertThat(typeName, is(not(INT_PRIMITIVE_TYPE)));
    }

    @Test
    @DisplayName("getStatusCode returns the status code provided in the constructor when such code is provided")
    public void getStatusCodeReturnsTheStatusCodeProvidedInTheConstructorWhenSuchStatusCodeisProvided() {
        IOException ioException = new IOException();
        Integer overideDefaultStatusCode = HttpStatus.SC_SEE_OTHER;
        TestException exception = new TestException(ioException, overideDefaultStatusCode);
        assertThat(exception.getStatusCode(), is(equalTo(overideDefaultStatusCode)));
    }

    private static class TestException extends ApiGatewayException {

        public TestException(String message) {
            super(message);
        }

        public TestException(Exception e) {
            super(e);
        }

        public TestException(Exception e, Integer statusCode) {
            super(e, statusCode);
        }

        @Override
        public Integer statusCode() {
            return 0;
        }
    }

    private static class ExceptionWithoutStatusCode extends ApiGatewayException {

        public ExceptionWithoutStatusCode(String message) {
            super(message);
        }

        @Override
        protected Integer statusCode() {
            return null;
        }
    }
}
