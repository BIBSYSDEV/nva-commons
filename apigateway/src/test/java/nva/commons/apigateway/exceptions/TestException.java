package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class TestException extends ApiGatewayException {

    public static final Integer ERROR_STATUS_CODE = HttpURLConnection.HTTP_NOT_FOUND;

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
        return ERROR_STATUS_CODE;
    }
}
