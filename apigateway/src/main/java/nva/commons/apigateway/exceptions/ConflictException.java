package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class ConflictException extends ApiGatewayException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(Exception exception) {
        super(exception);
    }

    public ConflictException(Exception exception, String message) {
        super(exception, message);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_CONFLICT;
    }
}
