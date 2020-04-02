package nva.commons.exceptions;

import org.apache.http.HttpStatus;

public class TestException extends ApiGatewayException {

    public TestException(String message) {
        super(message);
    }

    public TestException(Exception exc) {
        super(exc);
    }

    public TestException(Exception exc, String message) {
        super(exc, message);
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_NOT_FOUND;
    }
}
