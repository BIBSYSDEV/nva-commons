package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class BadRequestException extends ApiGatewayException {

    public BadRequestException(String message) {
        super(message);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_REQUEST;
    }
}
