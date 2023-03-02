package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class BadMethodException extends ApiGatewayException {

    public BadMethodException(String message) {
        super(message);
    }

    public BadMethodException(Exception exception) {
        super(exception);
    }

    public BadMethodException(Exception exception, String message) {
        super(exception, message);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_METHOD;
    }

}
