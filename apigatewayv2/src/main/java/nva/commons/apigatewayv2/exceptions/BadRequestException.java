package nva.commons.apigatewayv2.exceptions;

import java.net.HttpURLConnection;

public class BadRequestException extends ApiGatewayException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Exception cause) {
        super(cause, message);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_REQUEST;
    }
}
